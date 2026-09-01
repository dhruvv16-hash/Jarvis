package com.jarvispoc.history

interface ExecutionHistoryStore {
    suspend fun record(record: ExecutionRecord)
}
