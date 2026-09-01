package com.jarvispoc.memory

interface MemoryStore {
    suspend fun insert(item: MemoryItem)
    suspend fun search(query: String): List<MemoryItem>
}
