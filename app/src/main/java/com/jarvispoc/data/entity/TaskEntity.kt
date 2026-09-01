package com.jarvispoc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val goal: String,
    val status: String,
    val priority: Int,
    val currentStepIndex: Int,
    val requiresConfirmation: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val startedAt: Long?,
    val completedAt: Long?
)
