package com.jarvispoc.agent

enum class RequestSource {
    USER_DIRECT, USER_VOICE, NOTIFICATION, SCHEDULE, APP_CONTENT, SYSTEM_EVENT
}

data class AgentRequest(
    val requestId: String,
    val userId: String,
    val sessionId: String,
    val taskId: String?,
    val goal: Goal,
    val inputText: String,
    val source: RequestSource = RequestSource.USER_DIRECT,
    val metadata: Map<String, String> = emptyMap()
)
