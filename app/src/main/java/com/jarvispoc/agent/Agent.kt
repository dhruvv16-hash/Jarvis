package com.jarvispoc.agent

interface Agent {
    val id: String
    val name: String

    suspend fun run(request: AgentRequest): AgentResponse
}
