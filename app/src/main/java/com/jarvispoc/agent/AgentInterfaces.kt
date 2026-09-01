package com.jarvispoc.agent

import com.jarvispoc.execution.ExecutionResult
import com.jarvispoc.tools.ToolCall

interface AgentContextProvider {
    suspend fun buildContext(request: AgentRequest, observationSummary: String?): String
}

interface AgentPlanner {
    suspend fun decideNextAction(context: String, request: AgentRequest): PlannerDecision
}

interface AgentToolExecutor {
    suspend fun execute(toolCall: ToolCall): ExecutionResult
}
