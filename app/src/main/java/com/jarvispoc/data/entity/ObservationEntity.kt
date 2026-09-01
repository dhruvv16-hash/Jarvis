package com.jarvispoc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "observations")
data class ObservationEntity(
    @PrimaryKey val id: String,
    val executionId: String,
    val type: String,
    val appId: String?,
    val summary: String,
    val structuredDataJson: String,
    val timestamp: Long
)
