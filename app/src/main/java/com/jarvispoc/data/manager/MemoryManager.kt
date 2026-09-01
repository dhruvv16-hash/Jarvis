package com.jarvispoc.data.manager

import com.jarvispoc.data.entity.MemoryEntity
import com.jarvispoc.data.repository.MemoryRepository
import com.jarvispoc.memory.MemoryCategory
import com.jarvispoc.memory.MemoryItem
import com.jarvispoc.memory.preference.MemoryScope
import com.jarvispoc.memory.preference.MemorySource
import java.util.UUID

class MemoryManager(private val repository: MemoryRepository) : com.jarvispoc.memory.MemoryManager {
    override suspend fun remember(
        content: String, 
        category: MemoryCategory, 
        scope: MemoryScope,
        source: MemorySource,
        appId: String?, 
        capabilityId: String?,
        confidence: Float,
        expiresAt: Long?
    ): String {
        val existing = if (appId != null) {
            repository.getByApp(appId).filter { it.category == category && it.scope == scope.name && it.capabilityId == capabilityId }
        } else {
            repository.getByCategory(category).filter { it.scope == scope.name && it.capabilityId == capabilityId }
        }
        
        val match = existing.find { it.content == content }
        if (match != null) {
            repository.insert(match.copy(updatedAt = System.currentTimeMillis()))
            return match.id
        } else {
            val id = UUID.randomUUID().toString()
            repository.insert(MemoryEntity(
                id = id,
                content = content,
                category = category,
                scope = scope.name,
                source = source.name,
                appId = appId,
                capabilityId = capabilityId,
                importance = 1,
                confidence = confidence,
                expiresAt = expiresAt,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                active = true
            ))
            return id
        }
    }

    override suspend fun recall(query: String): List<MemoryItem> {
        return repository.search(query).map { it.toDomain() }
    }
    
    override suspend fun getRelevantPreferences(scope: MemoryScope, appId: String?, capabilityId: String?): List<MemoryItem> {
        val all = repository.getByCategory(MemoryCategory.PREFERENCE)
        val now = System.currentTimeMillis()
        
        return all.filter {
            it.active && 
            (it.expiresAt == null || it.expiresAt > now) &&
            (it.scope == MemoryScope.GLOBAL.name || 
             it.scope == scope.name || 
             (appId != null && it.appId == appId) || 
             (capabilityId != null && it.capabilityId == capabilityId))
        }.sortedByDescending { it.confidence }.map { it.toDomain() }
    }

    suspend fun forget(id: String) {
        repository.deleteById(id)
    }

    suspend fun list(category: MemoryCategory? = null): List<MemoryItem> {
        return if (category != null) {
            repository.getByCategory(category).map { it.toDomain() }
        } else {
            repository.getRecent(100).map { it.toDomain() }
        }
    }
    
    private fun MemoryEntity.toDomain() = MemoryItem(
        id = id,
        content = content,
        category = category,
        scope = MemoryScope.valueOf(scope),
        source = MemorySource.valueOf(source),
        appId = appId,
        capabilityId = capabilityId,
        confidence = confidence,
        timestamp = updatedAt,
        active = active
    )
}
