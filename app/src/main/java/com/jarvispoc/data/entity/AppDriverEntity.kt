package com.jarvispoc.data.entity

import androidx.room.Entity

@Entity(tableName = "app_drivers", primaryKeys = ["id"])
data class AppDriverEntity(
    val id: String,
    val appId: String,
    val capabilityId: String,
    val driverType: String,
    val enabled: Boolean,
    val confidence: Float,
    val lastVerifiedAt: Long?,
    val successCount: Int,
    val failureCount: Int
)
