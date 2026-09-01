package com.jarvispoc.agent

interface AgentRuntime {
    suspend fun process(request: AgentRequest): AgentResponse
}
