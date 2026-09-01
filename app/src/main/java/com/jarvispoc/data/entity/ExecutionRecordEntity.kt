package com.jarvispoc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "execution_history")
data class ExecutionRecordEntity(
    @PrimaryKey val id: String,
    val taskId: String?,
    val sessionId: String,
    val goal: String,
    val appId: String?,
    val capabilityId: String?,
    val driverType: String?,
    val status: String,
    val startedAt: Long,
    val completedAt: Long?,
    val duration: Long?,
    val failureReason: String?,
    val userConfirmed: Boolean,
    val createdAt: Long
)
