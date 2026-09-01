package com.jarvispoc.agent

import com.jarvispoc.agent.learning.ProcedureMatcher
import com.jarvispoc.model.LanguageModel
import com.jarvispoc.tools.ToolCall
import com.jarvispoc.tools.ToolRegistry
import java.util.UUID

class Planner(
    private val languageModel: LanguageModel,
    private val toolRegistry: ToolRegistry,
    private val procedureMatcher: ProcedureMatcher? = null
) : AgentPlanner {
    override suspend fun decideNextAction(context: String, goal: Goal, sessionId: String, taskId: String?): PlannerDecision {
        
        // 1. Deterministic Procedure Matching
        if (procedureMatcher != null) {
            val procedures = procedureMatcher.matchProcedures(goal, "current_app", null)
            val trustedProcedure = procedures.firstOrNull { it.confidence > 0.8f }
            
            if (trustedProcedure != null && trustedProcedure.steps.isNotEmpty()) {
                val step = trustedProcedure.steps.first() // simplified for turn-based Phase 5 proof
                val call = ToolCall(
                    id = UUID.randomUUID().toString(),
                    toolId = "execute_procedure_step",
                    capabilityId = trustedProcedure.capabilityId,
                    arguments = step.target,
                    taskId = taskId,
                    sessionId = sessionId
                )
                return PlannerDecision.Action(call)
            }
        }

        // 2. LLM Fallback (Generic Planning)
        val availableTools = toolRegistry.list().joinToString("\n") {
            "${it.name} (${it.id}): ${it.description}"
        }
        
        val output = languageModel.generateAction(context, availableTools)
        
        return when (output.type) {
            "tool_call" -> {
                if (output.toolCall != null) {
                    val enrichedCall = output.toolCall.copy(
                        sessionId = sessionId, 
                        taskId = taskId, 
                        id = UUID.randomUUID().toString()
                    )
                    PlannerDecision.Action(enrichedCall)
                } else {
                    PlannerDecision.Failure("Model returned tool_call type but no payload.")
                }
            }
            "ask_user" -> PlannerDecision.AskUser(output.question ?: "Please clarify.")
            "complete" -> PlannerDecision.Complete(output.summary ?: "Task completed successfully.")
            else -> PlannerDecision.Failure(output.error ?: "Invalid model output type: ${output.type}")
        }
    }
}

sealed class PlannerDecision {
    data class Action(val toolCall: ToolCall) : PlannerDecision()
    data class AskUser(val question: String) : PlannerDecision()
    data class Complete(val summary: String) : PlannerDecision()
    data class Failure(val error: String) : PlannerDecision()
}
