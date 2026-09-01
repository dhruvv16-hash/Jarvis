package com.jarvispoc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "procedures")
data class ProcedureEntity(
    @PrimaryKey val id: String,
    val name: String,
    val goalDescription: String,
    val appId: String,
    val capabilityId: String,
    val stepsJson: String,
    val conditionsJson: String,
    val fallbacksJson: String,
    val successCriteriaJson: String,
    val confidence: Float,
    val successCount: Int,
    val failureCount: Int,
    val lastSuccessAt: Long?,
    val lastFailureAt: Long?,
    val appVersion: String?,
    val createdAt: Long,
    val updatedAt: Long
)
