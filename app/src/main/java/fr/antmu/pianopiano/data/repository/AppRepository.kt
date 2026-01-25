package fr.antmu.pianopiano.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.data.model.ConfiguredApp

class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val preferencesManager = PreferencesManager(context)

    data class InstalledApp(
        val packageName: String,
        val appName: String,
        val icon: Drawable,
        val isConfigured: Boolean
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
            "com.google.android.apps.restore",
            "com.google.android.tts",                // Speech Services by Google
            "com.google.android.marvin.talkback",    // Android Accessibility Suite / TalkBack
            "com.google.android.accessibility.suite",
            "com.android.providers.accessibility",
            "com.google.android.apps.accessibility.voiceaccess", // Voice Access
            "com.google.android.speech.pumpkin",     // Speech Recognition
            "com.google.android.apps.speechservices",
            "com.google.android.googlequicksearchbox", // Google App (souvent utilisé pour speech)
            "com.android.soundpicker",               // Sound Picker
            "com.android.deskclock",                 // Clock
            "com.android.contacts",                  // Contacts (sauf si utilisé vraiment)
            "com.android.mms",                       // Messaging
            "com.android.camera2",                   // Camera
            "com.android.gallery3d",                 // Gallery
            "com.android.calculator2",               // Calculator
            "com.google.android.apps.wellbeingpreload", // Digital Wellbeing
            "com.android.egg",                       // Easter Egg
            "com.android.emergency",                 // Emergency
            "com.android.statementservice",
            "com.google.android.nearby.halfsheet",   // Nearby Share
            "com.google.android.apps.turbo",         // Device Health Services
            "com.google.android.apps.scone",         // Personal Safety
            "com.google.android.devicelockcontroller",
            "com.google.android.apps.work.oobconfig",
            "com.google.android.apps.subscription",
            "com.google.android.euicc",              // eSIM Manager
            "com.google.euicc",
            "com.google.android.ims",                // RCS/IMS
            "com.google.android.apps.restore",
            "com.google.android.settings.intelligence", // Settings Suggestions
            "com.google.android.apps.tips",          // Tips
            "com.google.android.apps.cameralite",
            "com.google.android.apps.nbu.files"      // Files by Google (si système)
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
            "com.google.android.ext.",
            "com.google.android.accessibility.",
            "com.google.android.tts.",
            "com.google.android.speech.",
            "com.android.launcher",        // Launchers système
            "com.sec.android.",            // Samsung system apps
            "com.samsung.android.",        // Samsung system apps
            "com.miui.",                   // Xiaomi MIUI
            "com.xiaomi.",                 // Xiaomi
            "com.huawei.",                 // Huawei
            "com.oppo.",                   // Oppo
            "com.vivo.",                   // Vivo
            "com.oneplus.",                // OnePlus
            "com.coloros."                 // ColorOS
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
                InstalledApp(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    icon = packageManager.getApplicationIcon(appInfo),
                    isConfigured = configuredPackages.contains(appInfo.packageName)
                )
            }

        // Trier: apps configurées en premier, puis alphabétique
        return apps.sortedWith(
            compareByDescending<InstalledApp> { it.isConfigured }
                .thenBy { it.appName.lowercase() }
        )
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
}
