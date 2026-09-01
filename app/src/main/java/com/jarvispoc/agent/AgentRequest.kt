package com.jarvispoc.agent

data class AgentRequest(
    val requestId: String,
    val userId: String,
    val sessionId: String,
    val taskId: String?,
    val goal: Goal,
    val inputText: String,
    val metadata: Map<String, String>
)
