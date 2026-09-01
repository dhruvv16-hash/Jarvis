package com.jarvispoc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_steps")
data class TaskStepEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val sequence: Int,
    val description: String,
    val capabilityId: String?,
    val status: String,
    val attemptCount: Int,
    val resultSummary: String?,
    val createdAt: Long,
    val updatedAt: Long
)
