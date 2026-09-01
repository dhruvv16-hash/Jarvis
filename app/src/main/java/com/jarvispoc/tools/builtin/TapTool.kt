package com.jarvispoc.tools.builtin

import com.jarvispoc.security.RiskClassification
import com.jarvispoc.tools.*
import com.jarvispoc.execution.CapabilityRouter
import com.jarvispoc.apps.AppCapability

class TapTool(private val router: CapabilityRouter) : Tool {
    override val id = "tool_tap"
    override val name = "Tap Element"
    override val description = "Taps a UI element on the screen."
    override val capabilities = setOf("device.interact.tap")
    override val riskLevel = RiskClassification.LOW

    override suspend fun execute(call: ToolCall): ToolResult {
        val result = router.route(call, AppCapability("device.interact.tap", ""), call.arguments["appId"] as? String ?: "")
        return ToolResult(call.id, result.status, result.summary, result.structuredData, result.error, retryable = result.retryable)
    }
}
