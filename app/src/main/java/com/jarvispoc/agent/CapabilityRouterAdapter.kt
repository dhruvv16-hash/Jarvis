package com.jarvispoc.agent

import com.jarvispoc.apps.AppCapability
import com.jarvispoc.execution.CapabilityRouter
import com.jarvispoc.execution.ExecutionResult
import com.jarvispoc.tools.ToolCall

class CapabilityRouterAdapter(
    private val capabilityRouter: CapabilityRouter
) : AgentToolExecutor {
    override suspend fun execute(toolCall: ToolCall): ExecutionResult {
        val capId = toolCall.capabilityId ?: "device.observe"
        val appId = toolCall.arguments["appId"] as? String ?: "system"
        return capabilityRouter.route(toolCall, AppCapability(capId, ""), appId)
    }
}
