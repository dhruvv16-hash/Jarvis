package com.jarvispoc.security

import com.jarvispoc.tools.ToolCall

interface ConfirmationManager {
    suspend fun requestConfirmation(action: ToolCall, risk: RiskClassification): Boolean
    fun requiresConfirmation(action: ToolCall, risk: RiskClassification): Boolean
}
