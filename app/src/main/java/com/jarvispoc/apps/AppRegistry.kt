package com.jarvispoc.apps

interface AppRegistry {
    suspend fun getApp(id: String): AppDescriptor?
    suspend fun findProviders(capability: String): List<AppDescriptor>
}
