package com.jarvispoc.agent

data class Goal(
    val id: String,
    val rawRequest: String,
    val objective: String,
    val constraints: Map<String, Any>,
    val preferences: Map<String, Any>,
    val createdAt: Long
)
