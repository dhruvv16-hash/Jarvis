package com.jarvispoc.testdoubles

import com.jarvispoc.agent.AgentToolExecutor
import com.jarvispoc.execution.ExecutionResult
import com.jarvispoc.tools.ToolCall

class FakeToolExecutor(
    private val results: List<ExecutionResult>
) : AgentToolExecutor {
    private var callIndex = 0
    val executedCalls = mutableListOf<ToolCall>()

    override suspend fun execute(toolCall: ToolCall): ExecutionResult {
        executedCalls.add(toolCall)
        if (callIndex < results.size) {
            return results[callIndex++]
        }
        throw IllegalStateException("FakeToolExecutor sequence exhausted")
    }
}
