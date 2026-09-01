package com.jarvispoc.agent

import com.jarvispoc.agent.learning.ProcedureMatcher
import com.jarvispoc.model.LanguageModel
import com.jarvispoc.model.ModelDecision
import com.jarvispoc.tools.ToolCall
import com.jarvispoc.tools.ToolRegistry
import java.util.UUID

class Planner(
    private val languageModel: LanguageModel,
    private val toolRegistry: ToolRegistry,
    private val procedureMatcher: ProcedureMatcher? = null
) : AgentPlanner {
    override suspend fun decideNextAction(context: String, request: AgentRequest): PlannerDecision {
        
        // 1. Deterministic Procedure Matching
        if (procedureMatcher != null) {
            val procedures = procedureMatcher.matchProcedures(request.goal, "current_app", null)
            val trustedProcedure = procedures.firstOrNull { it.confidence > 0.8f }
            
            if (trustedProcedure != null && trustedProcedure.steps.isNotEmpty()) {
                val step = trustedProcedure.steps.first() 
                val call = ToolCall(
                    id = UUID.randomUUID().toString(),
                    toolId = "execute_procedure_step",
                    capabilityId = trustedProcedure.capabilityId,
                    arguments = step.target,
                    taskId = request.taskId,
                    sessionId = request.sessionId
                )
                return PlannerDecision.Action(call)
            }
        }

        // 2. LLM Fallback (Generic Planning)
        val availableTools = toolRegistry.list().joinToString("\n") {
            " (): "
        }
        
        val decision = languageModel.generateAction(context, availableTools)
        
        return when (decision) {
            is ModelDecision.Action -> {
                val enrichedCall = decision.toolCall.copy(
                    sessionId = request.sessionId, 
                    taskId = request.taskId, 
                    id = UUID.randomUUID().toString()
                )
                PlannerDecision.Action(enrichedCall)
            }
            is ModelDecision.MemoryWrite -> {
                val call = ToolCall(
                    id = UUID.randomUUID().toString(),
                    toolId = "save_memory",
                    capabilityId = decision.request.capabilityId,
                    arguments = mapOf(
                        "type" to decision.request.type.name,
                        "content" to decision.request.content,
                        "category" to decision.request.category.name,
                        "scope" to decision.request.scope.name,
                        "appId" to (decision.request.appId ?: ""),
                        "capabilityId" to (decision.request.capabilityId ?: ""),
                        "confidence" to decision.request.confidence,
                        "source" to decision.request.source.name,
                        "requestSource" to request.source.name
                    ),
                    taskId = request.taskId,
                    sessionId = request.sessionId
                )
                PlannerDecision.Action(call)
            }
            is ModelDecision.AskUser -> PlannerDecision.AskUser(decision.question)
            is ModelDecision.Complete -> PlannerDecision.Complete(decision.summary)
            is ModelDecision.Invalid -> PlannerDecision.Failure(decision.error)
        }
    }
}

sealed class PlannerDecision {
    data class Action(val toolCall: ToolCall) : PlannerDecision()
    data class AskUser(val question: String) : PlannerDecision()
    data class Complete(val summary: String) : PlannerDecision()
    data class Failure(val error: String) : PlannerDecision()
}
