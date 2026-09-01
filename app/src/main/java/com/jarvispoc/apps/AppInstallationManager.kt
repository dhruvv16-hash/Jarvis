package com.jarvispoc.apps

interface InstallationDriver {
    suspend fun requestInstallation(appId: String, packageName: String): InstallationState
    suspend fun checkInstallationState(packageName: String): InstallationState
}

interface AppInstallationManager {
    suspend fun canInstall(appId: String): Boolean
    suspend fun requestInstallation(appId: String, taskId: String): InstallationState
    suspend fun getInstallationState(appId: String): InstallationState
    suspend fun observeInstallation(appId: String): InstallationState
    suspend fun cancelInstallation(appId: String)
}

class DefaultAppInstallationManager(
    private val appCatalog: AppCatalog,
    private val installationDriver: InstallationDriver
) : AppInstallationManager {
    
    private val activeInstalls = mutableMapOf<String, InstallationState>()

    override suspend fun canInstall(appId: String): Boolean {
        val details = appCatalog.getAppDetails(appId)
        return details != null && details.installationState != InstallationState.INSTALLED
    }

    override suspend fun requestInstallation(appId: String, taskId: String): InstallationState {
        val details = appCatalog.getAppDetails(appId) ?: return InstallationState.UNKNOWN
        val state = installationDriver.requestInstallation(appId, details.packageName)
        activeInstalls[appId] = state
        return state
    }

    override suspend fun getInstallationState(appId: String): InstallationState {
        val details = appCatalog.getAppDetails(appId) ?: return InstallationState.UNKNOWN
        return installationDriver.checkInstallationState(details.packageName)
    }

    override suspend fun observeInstallation(appId: String): InstallationState {
        return getInstallationState(appId)
    }

    override suspend fun cancelInstallation(appId: String) {
        // Driver dependent cancellation, omitted for simplicity
    }
}
