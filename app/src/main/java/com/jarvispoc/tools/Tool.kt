package com.jarvispoc.tools

import com.jarvispoc.security.RiskClassification

interface Tool {
    val id: String
    val name: String
    val description: String
    val capabilities: Set<String>
    val riskLevel: RiskClassification

    suspend fun execute(call: ToolCall): ToolResult
}
