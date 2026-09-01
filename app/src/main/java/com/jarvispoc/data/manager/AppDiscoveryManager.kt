package com.jarvispoc.data.manager

import android.content.Context
import android.content.pm.PackageManager
import com.jarvispoc.data.entity.AppEntity
import com.jarvispoc.data.repository.AppRepository
import java.util.UUID

class AppDiscoveryManager(private val context: Context, private val repository: AppRepository) {
    suspend fun refreshInstalledApps() {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        val existingApps = repository.getAllApps().associateBy { it.packageName }
        val now = System.currentTimeMillis()

        for (pkg in packages) {
            val packageName = pkg.packageName
            val appName = pkg.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: packageName
            val existing = existingApps[packageName]
            if (existing != null) {
                repository.insertApp(existing.copy(
                    versionName = pkg.versionName,
                    versionCode = pkg.longVersionCode,
                    installed = true,
                    lastSeenAt = now,
                    updatedAt = now
                ))
            } else {
                repository.insertApp(AppEntity(
                    id = UUID.randomUUID().toString(),
                    packageName = packageName,
                    displayName = appName,
                    versionName = pkg.versionName,
                    versionCode = pkg.longVersionCode,
                    installed = true,
                    enabled = true,
                    firstSeenAt = now,
                    lastSeenAt = now,
                    updatedAt = now
                ))
            }
        }

        // Mark missing apps as uninstalled
        for (existing in existingApps.values) {
            if (packages.none { it.packageName == existing.packageName }) {
                repository.insertApp(existing.copy(installed = false, updatedAt = now))
            }
        }
    }
}


