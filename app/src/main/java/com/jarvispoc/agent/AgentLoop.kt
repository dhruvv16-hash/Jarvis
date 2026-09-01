package com.jarvispoc.agent

import com.jarvispoc.agent.learning.AdaptationEngine
import com.jarvispoc.agent.learning.ProcedureLearner
import com.jarvispoc.agent.learning.ProcedureStep
import com.jarvispoc.execution.ExecutionResult
import com.jarvispoc.tools.ToolResultStatus
import kotlinx.coroutines.delay

class AgentLoop(
    private val contextProvider: AgentContextProvider,
    private val planner: AgentPlanner,
    private val toolExecutor: AgentToolExecutor,
    private val procedureLearner: ProcedureLearner? = null,
    private val adaptationEngine: AdaptationEngine? = null,
    private val maxTurns: Int = 10
) {
    suspend fun run(request: AgentRequest): AgentResponse {
        var turnCount = 0
        var currentObservation: String? = null
        var lastError: String? = null
        val executedSteps = mutableListOf<ProcedureStep>()

        while (turnCount < maxTurns) {
            turnCount++

            val context = contextProvider.buildContext(request, currentObservation)
            
            val decision = planner.decideNextAction(context, request.goal, request.sessionId, request.taskId)

            when (decision) {
                is PlannerDecision.Complete -> {
                    // Record successful procedure
                    if (executedSteps.isNotEmpty()) {
                        procedureLearner?.observeSuccess(
                            goal = request.goal.objective,
                            appId = "current_app", // In real system, inferred from context
                            capabilityId = "capability",
                            executedSteps = executedSteps,
                            appVersion = "1.0"
                        )
                    }
                    return AgentResponse(
                        summary = decision.summary,
                        taskId = request.taskId,
                        sessionId = request.sessionId,
                        status = AgentResponseStatus.SUCCESS,
                        requiresUserAction = false
                    )
                }
                is PlannerDecision.AskUser -> {
                    return AgentResponse(
                        summary = decision.question,
                        taskId = request.taskId,
                        sessionId = request.sessionId,
                        status = AgentResponseStatus.NEEDS_USER_INPUT,
                        requiresUserAction = true
                    )
                }
                is PlannerDecision.Failure -> {
                    lastError = decision.error
                    if (turnCount >= maxTurns) {
                        return AgentResponse(
                            summary = "Failed after max retries.",
                            taskId = request.taskId,
                            sessionId = request.sessionId,
                            status = AgentResponseStatus.FAILED,
                            requiresUserAction = false,
                            error = decision.error
                        )
                    }
                    continue
                }
                is PlannerDecision.Action -> {
                    val toolCall = decision.toolCall

                    val result: ExecutionResult = toolExecutor.execute(toolCall)

                    val capId = toolCall.capabilityId ?: "device.observe"
                    currentObservation = result.observations?.joinToString("\n") { it.text } 
                                         ?: "Action $capId executed with status ${result.status}"

                    when (result.status) {
                        ToolResultStatus.SUCCESS -> {
                            executedSteps.add(ProcedureStep(
                                sequence = turnCount,
                                action = toolCall.toolId,
                                target = toolCall.arguments as? Map<String, String> ?: emptyMap(),
                                expectedObservation = null,
                                verification = "success",
                                fallback = null
                            ))
                            lastError = null
                        }
                        ToolResultStatus.WAITING_FOR_USER -> {
                            return AgentResponse(
                                summary = result.summary,
                                taskId = request.taskId,
                                sessionId = request.sessionId,
                                status = AgentResponseStatus.WAITING_FOR_CONFIRMATION,
                                requiresUserAction = true
                            )
                        }
                        ToolResultStatus.APP_REQUIRED -> {
                            val candidateAppId = result.structuredData?.get("appId") as? String ?: "unknown_app"
                            return AgentResponse(
                                summary = "Required capability provider $candidateAppId is missing. Installation needed.",
                                taskId = request.taskId,
                                sessionId = request.sessionId,
                                status = AgentResponseStatus.WAITING_FOR_APP_INSTALL,
                                requiresUserAction = true,
                                error = candidateAppId
                            )
                        }
                        ToolResultStatus.FAILED -> {
                            procedureLearner?.observeFailure(
                                procedureId = "unknown",
                                appId = "current_app",
                                appVersion = "1.0",
                                failureReason = result.error ?: "Unknown error",
                                observation = currentObservation
                            )
                            
                            if (!result.retryable) {
                                return AgentResponse(
                                    summary = "Unrecoverable execution failure.",
                                    taskId = request.taskId,
                                    sessionId = request.sessionId,
                                    status = AgentResponseStatus.FAILED,
                                    requiresUserAction = false,
                                    error = result.error
                                )
                            }
                            lastError = result.error
                        }
                        else -> {
                            lastError = null
                        }
                    }
                }
            }
            delay(10)
        }

        return AgentResponse(
            summary = "Agent loop terminated after $maxTurns turns.",
            taskId = request.taskId,
            sessionId = request.sessionId,
            status = AgentResponseStatus.TIMEOUT,
            requiresUserAction = false
        )
    }
}
