package com.jarvispoc.security

import com.jarvispoc.tools.ToolCall

interface PolicyEngine {
    suspend fun evaluate(action: ToolCall): RiskClassification
}
