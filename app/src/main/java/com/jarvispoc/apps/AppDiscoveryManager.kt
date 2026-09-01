package com.jarvispoc.apps

interface AppDiscoveryManager {
    suspend fun discoverInstalled(): List<AppDescriptor>
}
