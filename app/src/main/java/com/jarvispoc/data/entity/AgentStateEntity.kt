package com.jarvispoc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_state")
data class AgentStateEntity(
    @PrimaryKey val id: Int = 1,
    val currentSessionId: String?,
    val currentTaskId: String?,
    val mode: String,
    val status: String,
    val lastActivityAt: Long
)
