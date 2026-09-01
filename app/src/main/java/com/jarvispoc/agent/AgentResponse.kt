package com.jarvispoc.agent

enum class AgentResponseStatus {
    SUCCESS, NEEDS_USER_INPUT, WAITING_FOR_CONFIRMATION, IN_PROGRESS, FAILED, CANCELLED, TIMEOUT, WAITING_FOR_APP_INSTALL, WAITING_FOR_APP_INITIALIZATION, WAITING_FOR_AUTHENTICATION
}

data class AgentResponse(
    val summary: String,
    val taskId: String?,
    val sessionId: String,
    val status: AgentResponseStatus,
    val requiresUserAction: Boolean,
    val error: String? = null
)
