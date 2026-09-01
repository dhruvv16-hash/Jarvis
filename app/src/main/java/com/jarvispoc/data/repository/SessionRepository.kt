package com.jarvispoc.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.jarvispoc.data.dao.SessionDao
import com.jarvispoc.data.entity.SessionEntity

class SessionRepository(private val sessionDao: SessionDao) {
    suspend fun insert(session: SessionEntity) = withContext(Dispatchers.IO) { sessionDao.insert(session) }
    suspend fun getById(id: String) = withContext(Dispatchers.IO) { sessionDao.getById(id) }
    suspend fun getActiveSession() = withContext(Dispatchers.IO) { sessionDao.getActiveSession() }
}

