package com.jarvispoc.history

data class ExecutionRecord(val id: String, val timestamp: Long, val goalId: String, val success: Boolean)
