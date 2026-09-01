package com.jarvispoc.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.jarvispoc.data.dao.MemoryDao
import com.jarvispoc.data.entity.MemoryEntity
import com.jarvispoc.memory.MemoryCategory

class MemoryRepository(private val memoryDao: MemoryDao) {
    suspend fun insert(memory: MemoryEntity) = withContext(Dispatchers.IO) { memoryDao.insert(memory) }
    suspend fun getById(id: String) = withContext(Dispatchers.IO) { memoryDao.getById(id) }
    suspend fun getByCategory(category: MemoryCategory) = withContext(Dispatchers.IO) { memoryDao.getByCategory(category) }
    suspend fun getByApp(appId: String) = withContext(Dispatchers.IO) { memoryDao.getByApp(appId) }
    suspend fun search(query: String) = withContext(Dispatchers.IO) { memoryDao.search(query) }
    suspend fun deleteById(id: String) = withContext(Dispatchers.IO) { memoryDao.deleteById(id) }
    suspend fun getRecent(limit: Int) = withContext(Dispatchers.IO) { memoryDao.getRecent(limit) }
}

