package com.jarvispoc.memory

interface MemoryExtractor {
    suspend fun extract(conversation: String): List<MemoryItem>
}
