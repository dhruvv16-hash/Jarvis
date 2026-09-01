package com.jarvispoc.data.manager

import com.jarvispoc.data.entity.MemoryEntity
import com.jarvispoc.data.repository.MemoryRepository
import com.jarvispoc.memory.MemoryCategory
import java.util.UUID

class MemoryManager(private val repository: MemoryRepository) {
    suspend fun remember(content: String, category: MemoryCategory, appId: String? = null, expiresAt: Long? = null) {
        // Deduplication logic
        val existing = if (appId != null) repository.getByApp(appId).filter { it.category == category } else repository.getByCategory(category)
        val match = existing.find { it.content == content }
        if (match != null) {
            repository.insert(match.copy(updatedAt = System.currentTimeMillis()))
        } else {
            repository.insert(MemoryEntity(
                id = UUID.randomUUID().toString(),
                content = content,
                category = category,
                source = "Agent",
                appId = appId,
                importance = 1,
                confidence = 1.0f,
                expiresAt = expiresAt,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    suspend fun recall(query: String): List<MemoryEntity> {
        return repository.search(query)
    }

    suspend fun forget(id: String) {
        repository.deleteById(id)
    }

    suspend fun list(category: MemoryCategory? = null): List<MemoryEntity> {
        return if (category != null) {
            repository.getByCategory(category)
        } else {
            repository.getRecent(100)
        }
    }
}
