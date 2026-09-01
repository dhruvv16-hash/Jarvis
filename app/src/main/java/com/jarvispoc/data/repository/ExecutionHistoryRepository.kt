package com.jarvispoc.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.jarvispoc.data.dao.ExecutionHistoryDao
import com.jarvispoc.data.entity.*

open class ExecutionHistoryRepository(private val dao: ExecutionHistoryDao) {
    open suspend fun insertExecution(record: ExecutionRecordEntity) = withContext(Dispatchers.IO) { dao.insertExecution(record) }
    suspend fun getExecution(id: String) = withContext(Dispatchers.IO) { dao.getExecution(id) }
    suspend fun insertActionAttempt(attempt: ActionAttemptEntity) = withContext(Dispatchers.IO) { dao.insertActionAttempt(attempt) }
    suspend fun insertObservation(observation: ObservationEntity) = withContext(Dispatchers.IO) { dao.insertObservation(observation) }
}


