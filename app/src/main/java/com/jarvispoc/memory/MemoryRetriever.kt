package com.jarvispoc.memory

import com.jarvispoc.agent.Goal

interface MemoryRetriever {
    suspend fun retrieveRelevantContext(goal: Goal): List<MemoryItem>
}
