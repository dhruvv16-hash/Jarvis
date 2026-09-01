package com.jarvispoc.memory

import com.jarvispoc.memory.preference.MemoryScope
import com.jarvispoc.memory.preference.MemorySource

data class MemoryItem(
    val id: String, 
    val content: String, 
    val category: MemoryCategory, 
    val scope: MemoryScope,
    val source: MemorySource,
    val appId: String?,
    val capabilityId: String?,
    val confidence: Float,
    val timestamp: Long,
    val active: Boolean
)
