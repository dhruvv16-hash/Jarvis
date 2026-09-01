package com.jarvispoc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Fts4
import com.jarvispoc.memory.MemoryCategory

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val content: String,
    val category: MemoryCategory,
    val source: String,
    val appId: String?,
    val importance: Int,
    val confidence: Float,
    val expiresAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "memories_fts")
@Fts4(contentEntity = MemoryEntity::class)
data class MemoryFtsEntity(
    val content: String
)
