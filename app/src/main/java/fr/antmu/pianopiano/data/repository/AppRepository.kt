package fr.antmu.pianopiano.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.data.model.ConfiguredApp

class AppRepository(context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val preferencesManager = PreferencesManager(context)

    data class InstalledApp(
        val packageName: String,
        val appName: String,
        val icon: Drawable,
        val isConfigured: Boolean
    )

    fun getInstalledUserApps(): List<InstalledApp> {
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val configuredApps = preferencesManager.getConfiguredApps()
        val configuredPackages = configuredApps.filter { it.isEnabled }.map { it.packageName }.toSet()

        return installedApps
            .filter { appInfo ->
                // Exclure notre app
                if (appInfo.packageName == "fr.antmu.pianopiano") return@filter false

                // Garder les apps non-système
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                // Garder : apps non-système OU apps système qui ont été mises à jour
                !isSystemApp || isUpdatedSystemApp
            }
            .map { appInfo ->
                InstalledApp(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    icon = packageManager.getApplicationIcon(appInfo),
                    isConfigured = configuredPackages.contains(appInfo.packageName)
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    fun setAppConfigured(packageName: String, appName: String, enabled: Boolean) {
        if (enabled) {
            preferencesManager.addConfiguredApp(ConfiguredApp(packageName, appName, true))
        } else {
            preferencesManager.removeConfiguredApp(packageName)
        }
    }

    fun isAppConfigured(packageName: String): Boolean {
        return preferencesManager.isAppConfigured(packageName)
    }

    fun getConfiguredApps(): List<ConfiguredApp> {
        return preferencesManager.getConfiguredApps().filter { it.isEnabled }
    }

    fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    // --- Periodic Timer Methods ---

    fun getAppPeriodicTimer(packageName: String): Int {
        return preferencesManager.getAppPeriodicTimer(packageName)
    }

    fun setAppPeriodicTimer(packageName: String, seconds: Int) {
        preferencesManager.setAppPeriodicTimer(packageName, seconds)
    }

    fun getLastActiveTimestamp(packageName: String): Long {
        return preferencesManager.getLastActiveTimestamp(packageName)
    }

    fun setLastActiveTimestamp(packageName: String, timestamp: Long) {
        preferencesManager.setLastActiveTimestamp(packageName, timestamp)
    }

    fun shouldResetTimer(packageName: String): Boolean {
        return preferencesManager.shouldResetTimer(packageName)
    }
}
