package com.jarvispoc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_attempts")
data class ActionAttemptEntity(
    @PrimaryKey val id: String,
    val executionId: String,
    val stepIndex: Int,
    val actionType: String,
    val target: String?,
    val driverType: String?,
    val argumentsJson: String,
    val result: String,
    val startedAt: Long,
    val completedAt: Long?
)
