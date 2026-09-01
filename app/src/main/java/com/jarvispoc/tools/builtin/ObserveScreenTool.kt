package com.jarvispoc.tools.builtin

import com.jarvispoc.security.RiskClassification
import com.jarvispoc.tools.*
import com.jarvispoc.execution.CapabilityRouter
import com.jarvispoc.apps.AppCapability

class ObserveScreenTool(private val router: CapabilityRouter) : Tool {
    override val id = "tool_observe_screen"
    override val name = "Observe Screen"
    override val description = "Captures the semantic state of the current screen."
    override val capabilities = setOf("device.observe")
    override val riskLevel = RiskClassification.LOW

    override suspend fun execute(call: ToolCall): ToolResult {
        val result = router.route(call, AppCapability("device.observe", ""), call.arguments["appId"] as? String ?: "")
        return ToolResult(call.id, result.status, result.summary, result.structuredData, result.error, retryable = result.retryable)
    }
}
