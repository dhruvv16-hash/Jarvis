package com.jarvispoc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val timezone: String,
    val locale: String,
    val createdAt: Long,
    val updatedAt: Long
)
