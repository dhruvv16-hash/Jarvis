package com.jarvispoc.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.jarvispoc.data.dao.AppDao
import com.jarvispoc.data.entity.*

class AppRepository(private val appDao: AppDao) {
    suspend fun insertApp(app: AppEntity) = withContext(Dispatchers.IO) { appDao.insertApp(app) }
    suspend fun getApp(id: String) = withContext(Dispatchers.IO) { appDao.getApp(id) }
    suspend fun getAllApps() = withContext(Dispatchers.IO) { appDao.getAllApps() }
    suspend fun insertCapability(capability: AppCapabilityEntity) = withContext(Dispatchers.IO) { appDao.insertCapability(capability) }
    suspend fun getCapabilities(appId: String) = withContext(Dispatchers.IO) { appDao.getCapabilities(appId) }
    suspend fun insertDriver(driver: AppDriverEntity) = withContext(Dispatchers.IO) { appDao.insertDriver(driver) }
    suspend fun getDrivers(appId: String) = withContext(Dispatchers.IO) { appDao.getDrivers(appId) }
    suspend fun insertState(state: AppStateEntity) = withContext(Dispatchers.IO) { appDao.insertState(state) }
    suspend fun getState(appId: String) = withContext(Dispatchers.IO) { appDao.getState(appId) }
}

