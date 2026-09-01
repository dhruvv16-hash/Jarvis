package com.jarvispoc.memory

import com.jarvispoc.memory.preference.MemoryScope
import com.jarvispoc.memory.preference.MemorySource

interface MemoryManager {
    suspend fun remember(
        content: String,
        category: MemoryCategory,
        scope: MemoryScope = MemoryScope.GLOBAL,
        source: MemorySource = MemorySource.USER_EXPLICIT,
        appId: String? = null,
        capabilityId: String? = null,
        confidence: Float = 1.0f,
        expiresAt: Long? = null
    ): String
    
    suspend fun recall(query: String): List<MemoryItem>
    
    suspend fun getRelevantPreferences(scope: MemoryScope, appId: String?, capabilityId: String?): List<MemoryItem>
}
