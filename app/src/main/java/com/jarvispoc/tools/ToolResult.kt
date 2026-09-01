package com.jarvispoc.tools

import com.jarvispoc.perception.Observation

enum class ToolResultStatus {
    SUCCESS, FAILED, WAITING_FOR_USER, WAITING_FOR_PERMISSION, UNAVAILABLE, RETRYABLE_FAILURE, APP_REQUIRED
}

data class ToolResult(
    val toolCallId: String,
    val status: ToolResultStatus,
    val summary: String,
    val structuredData: Map<String, Any>? = null,
    val error: String? = null,
    val observation: Observation? = null,
    val retryable: Boolean = false
)
