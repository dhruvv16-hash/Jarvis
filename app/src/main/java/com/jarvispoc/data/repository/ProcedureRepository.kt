package com.jarvispoc.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.jarvispoc.data.dao.ProcedureDao
import com.jarvispoc.data.entity.ProcedureEntity

class ProcedureRepository(private val dao: ProcedureDao) {
    suspend fun insert(procedure: ProcedureEntity) = withContext(Dispatchers.IO) { dao.insert(procedure) }
    suspend fun getById(id: String) = withContext(Dispatchers.IO) { dao.getById(id) }
    suspend fun getByApp(appId: String) = withContext(Dispatchers.IO) { dao.getByApp(appId) }
}

