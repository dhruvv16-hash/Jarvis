package com.jarvispoc.data.manager

import com.jarvispoc.data.entity.AppEntity
import com.jarvispoc.data.repository.AppRepository

open class AppRegistry(private val repository: AppRepository) {
    open suspend fun getApp(id: String) = repository.getApp(id)
    suspend fun getAllApps() = repository.getAllApps()
    suspend fun getCapabilities(appId: String) = repository.getCapabilities(appId)
    open suspend fun getDrivers(appId: String) = repository.getDrivers(appId)
    suspend fun getState(appId: String) = repository.getState(appId)
}


