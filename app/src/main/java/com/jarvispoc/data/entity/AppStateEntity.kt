package com.jarvispoc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val appId: String,
    val installed: Boolean,
    val loggedIn: Boolean,
    val currentScreen: String?,
    val currentActivity: String?,
    val stateJson: String,
    val lastKnownUiSignature: String?,
    val updatedAt: Long
)
