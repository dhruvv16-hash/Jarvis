package com.jarvispoc.agent

import com.jarvispoc.data.manager.AppRegistry
import com.jarvispoc.data.manager.MemoryManager
import com.jarvispoc.data.repository.ExecutionHistoryRepository
import com.jarvispoc.data.repository.SessionRepository
import com.jarvispoc.data.repository.TaskRepository

class ContextBuilder(
    private val memoryManager: MemoryManager,
    private val appRegistry: AppRegistry,
    private val historyRepo: ExecutionHistoryRepository,
    private val sessionRepo: SessionRepository,
    private val taskRepo: TaskRepository
) : AgentContextProvider {
    override suspend fun buildContext(request: AgentRequest, observationSummary: String?): String {
        val goalContext = "GOAL: ${request.goal.objective}\n"

        val memories = memoryManager.recall(request.goal.objective).take(5)
        val memoryContext = "MEMORIES:\n" + memories.joinToString("\n") { it.content } + "\n"

        val apps = appRegistry.getAllApps().filter { it.installed }.take(10)
        val appContext = "INSTALLED APPS:\n" + apps.joinToString("\n") { 
            "${it.displayName} (${it.packageName})" 
        } + "\n"

        val obsContext = if (observationSummary != null) "CURRENT OBSERVATION:\n$observationSummary\n" else ""

        return goalContext + memoryContext + appContext + obsContext
    }
}
