package com.jarvispoc.model

import com.jarvispoc.tools.ToolCall

interface LanguageModel {
    suspend fun generateAction(context: String, availableTools: String): ModelOutput
}

data class ModelOutput(
    val type: String, // "tool_call", "ask_user", "complete", "invalid"
    val toolCall: ToolCall? = null,
    val question: String? = null,
    val summary: String? = null,
    val error: String? = null
)
