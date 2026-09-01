package com.jarvispoc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val startedAt: Long,
    val lastActiveAt: Long,
    val status: String,
    val summary: String?,
    val activeTaskId: String?
)
