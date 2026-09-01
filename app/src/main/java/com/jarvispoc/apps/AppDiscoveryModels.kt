package com.jarvispoc.apps

enum class InstallationState {
    UNKNOWN,
    NOT_INSTALLED,
    DISCOVERED,
    USER_CONFIRMATION_REQUIRED,
    INSTALL_REQUESTED,
    INSTALLING,
    INSTALLED,
    INSTALL_FAILED,
    INSTALL_CANCELLED,
    INSTALL_BLOCKED
}

data class AppDiscoveryResult(
    val appId: String,
    val packageName: String,
    val provider: String,
    val capabilities: List<AppCapability>,
    val installationState: InstallationState,
    val confidence: Float
)

interface AppCatalog {
    suspend fun findProviders(capabilityId: String): List<AppDiscoveryResult>
    suspend fun getAppDetails(appId: String): AppDiscoveryResult?
}

data class CapabilityProvider(
    val capabilityId: String,
    val knownApps: List<String>
)
