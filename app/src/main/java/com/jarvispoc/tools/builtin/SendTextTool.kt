package com.jarvispoc.tools.builtin

import com.jarvispoc.security.RiskClassification
import com.jarvispoc.tools.*
import com.jarvispoc.execution.CapabilityRouter
import com.jarvispoc.apps.AppCapability

class SendTextTool(private val router: CapabilityRouter) : Tool {
    override val id = "tool_send_text"
    override val name = "Send Message"
    override val description = "Sends a text message using the specified app."
    override val capabilities = setOf("messaging.send")
    override val riskLevel = RiskClassification.MEDIUM

    override suspend fun execute(call: ToolCall): ToolResult {
        val result = router.route(call, AppCapability("messaging.send", ""), call.arguments["appId"] as? String ?: "")
        return ToolResult(call.id, result.status, result.summary, result.structuredData, result.error, retryable = result.retryable)
    }
}
