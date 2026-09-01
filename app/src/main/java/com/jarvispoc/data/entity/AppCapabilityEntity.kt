package com.jarvispoc.data.entity

import androidx.room.Entity

@Entity(tableName = "app_capabilities", primaryKeys = ["appId", "capabilityId"])
data class AppCapabilityEntity(
    val appId: String,
    val capabilityId: String,
    val displayName: String,
    val description: String,
    val enabled: Boolean,
    val confidence: Float,
    val createdAt: Long,
    val updatedAt: Long
)
