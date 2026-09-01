package com.jarvispoc.memory

interface MemoryManager {
    suspend fun remember(content: String, category: MemoryCategory)
    suspend fun recall(query: String): List<MemoryItem>
}
