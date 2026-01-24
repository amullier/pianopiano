package fr.antmu.pianopiano.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.data.model.ConfiguredApp
import fr.antmu.pianopiano.util.PermissionHelper
import fr.antmu.pianopiano.util.UsageStatsHelper

class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val preferencesManager = PreferencesManager(context)

    data class InstalledApp(
        val packageName: String,
        val appName: String,
        val icon: Drawable,
        val isConfigured: Boolean,
        val screenTimeToday: Long = 0L // Temps d'écran aujourd'hui en ms
    )

    companion object {
        // Packages système à exclure
        private val EXCLUDED_PACKAGES = setOf(
            "com.google.android.gms",           // Google Play Services
            "com.google.android.gsf",           // Google Services Framework
            "com.google.android.webview",       // Android System WebView
            "com.android.webview",
            "com.android.vending",              // Google Play Store
            "com.google.android.packageinstaller",
            "com.android.providers",
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.incallui",
            "com.android.mms.service",
            "com.android.stk",
            "com.android.cellbroadcastreceiver",
            "com.android.carrierconfig",
            "com.android.bluetooth",
            "com.android.nfc",
            "com.android.se",
            "com.android.providers.downloads",
            "com.android.providers.media",
            "com.android.providers.calendar",
            "com.android.providers.contacts",
            "com.android.providers.telephony",
            "com.android.documentsui",
            "com.android.externalstorage",
            "com.android.htmlviewer",
            "com.android.companiondevicemanager",
            "com.android.shell",
            "com.android.wallpaper",
            "com.android.inputdevices",
            "com.android.printspooler",
            "com.android.dreams.basic",
            "com.android.certinstaller",
            "com.android.carrierdefaultapp",
            "com.android.hotspot2",
            "com.android.cts.ctsshim",
            "com.google.android.projection.gearhead", // Android Auto
            "com.google.android.ext.services",
            "com.google.android.ext.shared",
            "com.google.android.onetimeinitializer",
            "com.google.android.partnersetup",
            "com.google.android.printservice.recommendation",
            "com.google.android.syncadapters",
            "com.google.android.backuptransport",
            "com.google.android.configupdater",
            "com.google.android.feedback",
            "com.google.android.setupwizard",
            "com.google.android.apps.wellbeing",     // Bien-être numérique
            "com.android.managedprovisioning",       // Provisioning
            "com.android.simappdialog",              // Gestionnaire de SIM
            "com.google.android.as",                 // Android System Intelligence
            "com.google.android.privatecomputeservices", // Private Compute Services
            "com.android.networkstack.tethering",    // Network Stack
            "com.google.mainline.telemetry",         // Main Components
            "com.android.systemui.plugin",
            "com.android.keychain",                  // Android System Key Verifier
            "com.google.android.modulemetadata",     // Support Components
            "com.google.android.networkstack",       // Gestionnaire de réseau
            "com.google.android.adservices.api",
            "com.google.android.captiveportallogin",
            "com.google.android.cellbroadcastservice",
            "com.google.android.permissioncontroller",
            "com.android.safetycenter.resources",    // System Safety Core
            "com.google.android.inputmethod.latin",  // Gboard
            "com.android.ons",                       // Android Switch
            "com.google.android.overlay",
            "com.google.android.documentsui",
            "com.android.carrierdefaultapp",
            "com.android.traceur",
            "com.google.android.apps.restore"
        )

        // Préfixes de packages système à exclure
        private val EXCLUDED_PREFIXES = listOf(
            "com.android.providers.",
            "com.android.internal.",
            "com.google.android.providers.",
            "com.samsung.android.providers.",
            "com.sec.android.providers.",
            "com.qualcomm.",
            "com.qti.",
            "com.mediatek.",
            "com.google.mainline.",
            "com.android.systemui.",
            "com.google.android.ext."
        )

        // Apps système populaires à toujours inclure (même si pré-installées)
        private val WHITELISTED_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.music",
            "com.google.android.apps.maps",
            "com.google.android.apps.photos",
            "com.google.android.gm",              // Gmail
            "com.google.android.apps.docs",
            "com.google.android.apps.messaging",
            "com.google.android.calendar",
            "com.google.android.contacts",
            "com.google.android.dialer",
            "com.android.chrome",
            "com.google.android.apps.tachyon",    // Google Duo/Meet
            "com.google.android.apps.meetings",   // Google Meet
            "com.netflix.mediaclient",
            "com.amazon.avod.thirdpartyclient",   // Prime Video
            "com.disney.disneyplus",
            "com.spotify.music",
            "com.zhiliaoapp.musically",           // TikTok
            "com.instagram.android",
            "com.facebook.katana",
            "com.facebook.orca",                  // Messenger
            "com.whatsapp",
            "com.twitter.android",
            "com.snapchat.android",
            "com.linkedin.android",
            "com.pinterest",
            "com.reddit.frontpage"
        )
    }

    fun getInstalledUserApps(): List<InstalledApp> {
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val configuredApps = preferencesManager.getConfiguredApps()
        val configuredPackages = configuredApps.filter { it.isEnabled }.map { it.packageName }.toSet()
        val hasUsageStats = PermissionHelper.hasUsageStatsPermission(context)

        val apps = installedApps
            .filter { appInfo ->
                // Exclure notre app
                if (appInfo.packageName == "fr.antmu.pianopiano") return@filter false

                // Toujours inclure les apps populaires (whitelist)
                if (WHITELISTED_PACKAGES.contains(appInfo.packageName)) return@filter true

                // Exclure les packages système spécifiques
                if (EXCLUDED_PACKAGES.contains(appInfo.packageName)) return@filter false

                // Exclure les packages avec certains préfixes
                if (EXCLUDED_PREFIXES.any { appInfo.packageName.startsWith(it) }) return@filter false

                // Garder les apps non-système
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                // Garder : apps non-système OU apps système qui ont été mises à jour
                !isSystemApp || isUpdatedSystemApp
            }
            .map { appInfo ->
                val screenTime = if (hasUsageStats) {
                    UsageStatsHelper.getAppUsageStats(context, appInfo.packageName)?.totalTimeToday ?: 0L
                } else {
                    0L
                }
                InstalledApp(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    icon = packageManager.getApplicationIcon(appInfo),
                    isConfigured = configuredPackages.contains(appInfo.packageName),
                    screenTimeToday = screenTime
                )
            }

        // Trier par temps d'écran si disponible, sinon par nom
        return if (hasUsageStats) {
            apps.sortedByDescending { it.screenTimeToday }
        } else {
            apps.sortedBy { it.appName.lowercase() }
        }
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

    fun getConfiguredAppPackages(): List<String> {
        return getConfiguredApps().map { it.packageName }
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
