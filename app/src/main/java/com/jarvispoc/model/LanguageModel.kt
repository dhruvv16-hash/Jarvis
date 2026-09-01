package com.jarvispoc.model

import com.jarvispoc.tools.ToolCall
import com.jarvispoc.memory.MemoryCategory
import com.jarvispoc.memory.preference.MemoryScope
import com.jarvispoc.memory.preference.MemorySource

sealed class ModelDecision {
    data class Action(val toolCall: ToolCall) : ModelDecision()
    data class MemoryWrite(val request: MemoryWriteRequest) : ModelDecision()
    data class AskUser(val question: String) : ModelDecision()
    data class Complete(val summary: String) : ModelDecision()
    data class Invalid(val error: String) : ModelDecision()
}

enum class MemoryWriteType { PREFERENCE, FACT, INSTRUCTION }

data class MemoryWriteRequest(
    val type: MemoryWriteType,
    val content: String,
    val category: MemoryCategory,
    val scope: MemoryScope,
    val appId: String? = null,
    val capabilityId: String? = null,
    val confidence: Float,
    val source: MemorySource
)

interface LanguageModel {
    suspend fun generateAction(context: String, availableTools: String): ModelDecision
}
