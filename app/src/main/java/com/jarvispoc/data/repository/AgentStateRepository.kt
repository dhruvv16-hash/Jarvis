package com.jarvispoc.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.jarvispoc.data.dao.AgentStateDao
import com.jarvispoc.data.entity.AgentStateEntity

class AgentStateRepository(private val dao: AgentStateDao) {
    suspend fun updateState(state: AgentStateEntity) = withContext(Dispatchers.IO) { dao.updateState(state) }
    suspend fun getState() = withContext(Dispatchers.IO) { dao.getState() }
}

