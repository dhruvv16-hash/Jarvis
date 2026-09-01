package com.jarvispoc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val displayName: String,
    val versionName: String?,
    val versionCode: Long?,
    val installed: Boolean,
    val enabled: Boolean,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val updatedAt: Long
)
