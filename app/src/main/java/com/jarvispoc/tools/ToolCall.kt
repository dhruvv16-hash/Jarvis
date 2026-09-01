package com.jarvispoc.tools

data class ToolCall(
    val id: String,
    val toolId: String,
    val capabilityId: String?,
    val arguments: Map<String, Any>,
    val taskId: String?,
    val sessionId: String?,
    val metadata: Map<String, String>? = null
)
