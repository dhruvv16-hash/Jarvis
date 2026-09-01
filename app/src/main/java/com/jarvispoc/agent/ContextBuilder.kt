package com.jarvispoc.agent

import com.jarvispoc.data.manager.AppRegistry
import com.jarvispoc.memory.MemoryManager
import com.jarvispoc.memory.preference.MemoryScope
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
        val goalContext = "GOAL: {request.goal.objective}\n"

        // Old recall
        val semanticMemories = memoryManager.recall(request.goal.objective).take(3)
        val memoryContext = "SEMANTIC MEMORIES:\n" + semanticMemories.joinToString("\n") { it.content } + "\n"

        // NEW: Preference recall based on capability
        // Very basic capability extraction for context (could be improved)
        val isShopping = request.goal.objective.lowercase().contains("order") || request.goal.objective.lowercase().contains("buy") || request.goal.objective.lowercase().contains("shop")
        val capability = if (isShopping) "shopping.order" else null
        
        val preferences = memoryManager.getRelevantPreferences(MemoryScope.CAPABILITY, null, capability)
        val preferenceContext = if (preferences.isNotEmpty()) {
            "USER PREFERENCES:\n" + preferences.joinToString("\n") { it.content } + "\n"
        } else ""

        val apps = appRegistry.getAllApps().filter { it.installed }.take(10)
        val appContext = "INSTALLED APPS:\n" + apps.joinToString("\n") { 
            "{it.displayName} ({it.packageName})" 
        } + "\n"

        val obsContext = if (observationSummary != null) "CURRENT OBSERVATION:\nobservationSummary\n" else ""

        return goalContext + memoryContext + preferenceContext + appContext + obsContext
    }
}
