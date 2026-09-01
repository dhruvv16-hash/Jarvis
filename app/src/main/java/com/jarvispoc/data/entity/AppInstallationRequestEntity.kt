package com.jarvispoc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_installation_requests")
data class AppInstallationRequestEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val sessionId: String,
    val appId: String,
    val packageName: String,
    val source: String,
    val status: String,
    val requestedAt: Long,
    val startedAt: Long?,
    val completedAt: Long?,
    val failureReason: String?
)
