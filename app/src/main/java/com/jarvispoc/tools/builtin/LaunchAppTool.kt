package com.jarvispoc.tools.builtin

import com.jarvispoc.security.RiskClassification
import com.jarvispoc.tools.*
import com.jarvispoc.execution.CapabilityRouter
import com.jarvispoc.apps.AppCapability

class LaunchAppTool(private val router: CapabilityRouter) : Tool {
    override val id = "tool_launch_app"
    override val name = "Launch App"
    override val description = "Launches a specific application."
    override val capabilities = setOf("device.launch_app")
    override val riskLevel = RiskClassification.LOW

    override suspend fun execute(call: ToolCall): ToolResult {
        val result = router.route(call, AppCapability("device.launch_app", ""), call.arguments["appId"] as? String ?: "")
        return ToolResult(call.id, result.status, result.summary, result.structuredData, result.error, retryable = result.retryable)
    }
}
