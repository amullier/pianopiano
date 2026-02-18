package fr.antmu.pianopiano.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import fr.antmu.pianopiano.BuildConfig
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.data.repository.AppRepository

class AppLaunchDetectorService : AccessibilityService() {

    companion object {
        private const val TAG = "AppLaunchDetector"

        /**
         * Package pour lequel une PauseActivity est actuellement affichée.
         * Mis à jour par le service (quand il lance la pause) et par PauseActivity (quand l'user fait un choix).
         * Volatile car accédé depuis le main thread par PauseActivity et le thread du service.
         */
        @Volatile
        var activePauseForPackage: String? = null
    }

    private fun logd(msg: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, msg)
    }

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var appRepository: AppRepository

    private var currentForegroundPackage: String? = null

    // Debounce pour ignorer les réentrées rapides (transition de fenêtre interne, ex: sortie plein écran)
    private val recentExitTimes = mutableMapOf<String, Long>()
    private val DEBOUNCE_DELAY_MS = 500L

    override fun onServiceConnected() {
        super.onServiceConnected()
        preferencesManager = PreferencesManager(applicationContext)
        appRepository = AppRepository(applicationContext)
        PeriodicTimerManager.restoreTimerIfNeeded(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        logd("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logd("onAccessibilityEvent() appelé")
        logd("Event reçu: $event")
        logd("Event type: ${event?.eventType}")
        logd("Event type name: ${event?.eventType?.let { AccessibilityEvent.eventTypeToString(it) }}")
        logd("Package name: ${event?.packageName}")
        logd("Class name: ${event?.className}")
        logd("Content description: ${event?.contentDescription}")
        logd("Text: ${event?.text}")
        logd("Source: ${event?.source}")

        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            logd("❌ Event ignoré: pas TYPE_WINDOW_STATE_CHANGED (type=${event?.eventType})")
            return
        }
        logd("✅ Event TYPE_WINDOW_STATE_CHANGED accepté")

        val newPkg = event.packageName?.toString()
        if (newPkg == null) {
            logd("❌ Package name est null, on ignore")
            return
        }
        logd("📦 Nouveau package: $newPkg")

        val now = System.currentTimeMillis()
        logd("⏱️ Timestamp actuel: $now")
        logd("📍 currentForegroundPackage: $currentForegroundPackage")

        // 🛡️ Détection : l'utilisateur a quitté PauseActivity sans faire de choix
        val pausePkg = activePauseForPackage
        if (pausePkg != null && newPkg != "fr.antmu.pianopiano"
            && newPkg != pausePkg
            && !isTemporaryOverlay(newPkg, event.source?.window)) {
            // Une vraie app (ou launcher) est apparue pendant que la pause était active
            // → l'utilisateur a quitté sans cliquer Cancel ni Continue
            logd("⚠️ PauseActivity quittée sans choix (pause pour $pausePkg, event=$newPkg)")
            preferencesManager.setForceNextPause(pausePkg, true)
            activePauseForPackage = null
        }

        // 🔒 Transition interne → IGNORER sauf si forceNextPause est actif
        if (newPkg == currentForegroundPackage) {
            // Vérifier si forceNextPause a été posé (par la détection ci-dessus ou par PauseActivity.onStop)
            if (preferencesManager.isAppConfigured(newPkg) && preferencesManager.shouldForceNextPause(newPkg)) {
                logd("🔒 Transition interne MAIS forceNextPause actif → relance pause")
                preferencesManager.setForceNextPause(newPkg, false)
                activePauseForPackage = newPkg
                ServiceHelper.startPauseOverlay(applicationContext, newPkg, isPeriodic = false)
                return
            }
            logd("🔒 Transition interne détectée (même package), on ignore")
            return
        }
        logd("✅ Changement d'app détecté: $currentForegroundPackage → $newPkg")

        // 👉 C'est un vrai changement d'application
        val previousPkg = currentForegroundPackage
        logd("📤 Package précédent: $previousPkg")

        // 🎹 Overlay temporaire (clavier, systemui, notre app) → ignorer complètement
        if (isTemporaryOverlay(newPkg, event.source?.window)) {
            logd("🎹 Overlay temporaire détecté ($newPkg), on ignore complètement")
            return
        }

        // 🏠 Launcher/Home → traiter comme sortie d'app
        if (isSystemApp(newPkg)) {
            logd("🏠 Launcher détecté ($newPkg), traitement comme sortie d'app")
            currentForegroundPackage = null
            handleAppExit(previousPkg, now)
            return
        }
        logd("✅ App normale détectée, on continue le traitement")

        currentForegroundPackage = newPkg
        logd("📍 currentForegroundPackage mis à jour: $currentForegroundPackage")

        // 1️⃣ Gérer la sortie de l'app précédente
        logd("1️⃣ Appel handleAppExit($previousPkg, $now)")
        handleAppExit(previousPkg, now)

        // 2️⃣ Si app non configurée → rien
        val isConfigured = preferencesManager.isAppConfigured(newPkg)
        logd("2️⃣ isAppConfigured($newPkg) = $isConfigured")
        if (!isConfigured) {
            logd("❌ App non configurée, on s'arrête là")
            return
        }
        logd("✅ App configurée, on continue")

        // Check force pause (après "Annuler" ou quand l'user a quitté PauseActivity sans choix)
        val forceNextPause = preferencesManager.shouldForceNextPause(newPkg)
        if (forceNextPause) {
            logd("🎯 Force pause activé, bypass debounce")
            preferencesManager.setForceNextPause(newPkg, false)  // Consommer le flag
            preferencesManager.setAppEnterTime(newPkg, now)
            activePauseForPackage = newPkg
            ServiceHelper.startPauseOverlay(applicationContext, newPkg, isPeriodic = false)
            return
        }

        // Check debounce : ignorer les réentrées rapides (transition de fenêtre interne, ex: sortie plein écran)
        val recentExitTime = recentExitTimes[newPkg] ?: 0L
        if ((now - recentExitTime) < DEBOUNCE_DELAY_MS) {
            logd("🔄 Réentrée rapide détectée (${now - recentExitTime}ms < ${DEBOUNCE_DELAY_MS}ms), transition interne ignorée")
            return
        }

        // 3️⃣ Décider si pause initiale
        val lastEnterTime = preferencesManager.getAppEnterTime(newPkg)
        val lastExitTime = preferencesManager.getAppExitTime(newPkg)
        logd("3️⃣ Récupération des temps:")
        logd("   lastEnterTime: $lastEnterTime")
        logd("   lastExitTime: $lastExitTime")
        logd("   now: $now")
        logd("   (now - lastExitTime): ${now - lastExitTime}ms")

        val shouldInitialPause = when {
            lastEnterTime == 0L -> {
                logd("   → shouldInitialPause=true (lastEnterTime == 0L, première fois)")
                true
            }
            (now - lastExitTime) > 10_000 -> {
                logd("   → shouldInitialPause=true ((now - lastExitTime) > 10000ms)")
                true
            }
            else -> {
                logd("   → shouldInitialPause=false (retour rapide dans l'app)")
                false
            }
        }
        logd("   Décision finale: shouldInitialPause = $shouldInitialPause")

        // 4️⃣ Mettre à jour le temps d'entrée
        logd("4️⃣ setAppEnterTime($newPkg, $now)")
        preferencesManager.setAppEnterTime(newPkg, now)

        // 5️⃣ Afficher pause si nécessaire
        logd("5️⃣ Action finale:")
        if (shouldInitialPause) {
            logd("   🎯 Démarrage PauseOverlay (isPeriodic=false)")
            activePauseForPackage = newPkg
            ServiceHelper.startPauseOverlay(applicationContext, newPkg, isPeriodic = false)
        } else {
            logd("   ⏰ Pas de pause initiale, vérification timer périodique")
            // Pas de pause initiale, démarrer le timer périodique si configuré
            startPeriodicTimerIfNeeded(newPkg)
        }
        logd("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /**
     * Overlays temporaires à ignorer complètement (pas de pause, pas de changement d'état)
     */
    private fun isTemporaryOverlay(packageName: String, windowType: AccessibilityWindowInfo?): Boolean {

        val isPackageDetectedAsAnOverlay = packageName == "fr.antmu.pianopiano" ||
                // System UI (notifications, volume, quick settings, heads-up)
                packageName == "com.android.systemui" ||
                packageName == "com.samsung.android.systemui" ||
                packageName == "com.miui.securitycenter" ||
                // Claviers
                packageName.contains("inputmethod") ||
                packageName.contains("keyboard") ||
                packageName == "com.google.android.inputmethod.latin" ||  // Gboard
                packageName == "com.samsung.android.honeyboard" ||         // Samsung Keyboard
                packageName == "com.touchtype.swiftkey" ||                 // SwiftKey
                packageName == "com.sec.android.inputmethod" ||            // Samsung ancien clavier
                // Assistants vocaux
                packageName == "com.google.android.googlequicksearchbox" || // Google Assistant
                packageName == "com.samsung.android.bixby.agent" ||
                packageName == "com.samsung.android.visionintelligence" ||
                // Popups système
                packageName == "com.android.permissioncontroller" ||       // Demandes de permission
                packageName == "com.google.android.permissioncontroller" ||
                packageName == "com.android.packageinstaller" ||           // Installation d'apps
                packageName == "com.google.android.packageinstaller" ||
                packageName == "com.samsung.android.packageinstaller" ||
                packageName == "com.miui.packageinstaller" ||
                // Partage / Sélecteur
                packageName == "android" ||                                // Intent chooser système
                packageName == "com.android.intentresolver" ||
                packageName == "com.samsung.android.app.sharelive" ||
                // Appels / Communications
                packageName.contains("incallui") ||                        // Écran d'appel
                packageName.contains("dialer") ||
                packageName == "com.samsung.android.incallui" ||
                packageName == "com.google.android.dialer" ||
                // Autres overlays
                packageName == "com.android.settings" ||                   // Paramètres rapides
                packageName == "com.android.documentsui" ||                // Sélecteur de fichiers
                packageName == "com.google.android.documentsui"

        return isPackageDetectedAsAnOverlay || when (windowType?.type) {
            AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY,
            AccessibilityWindowInfo.TYPE_MAGNIFICATION_OVERLAY,
            AccessibilityWindowInfo.TYPE_SYSTEM -> true
            else -> false
        }
    }

    /**
     * Launchers/Home = sortie d'app (déclenche handleAppExit)
     */
    private fun isSystemApp(packageName: String): Boolean {
        return packageName.startsWith("com.android.launcher") ||
                packageName.startsWith("com.google.android.launcher") ||
                packageName.startsWith("com.sec.android.app.launcher") ||
                packageName.startsWith("com.miui.home") ||
                packageName.startsWith("com.huawei.android.launcher") ||
                packageName.startsWith("com.oppo.launcher") ||
                packageName.startsWith("com.vivo.launcher") ||
                packageName.startsWith("com.oneplus.launcher") ||
                packageName.startsWith("com.realme.launcher") ||
                packageName.startsWith("com.asus.launcher") ||
                packageName.startsWith("com.lge.launcher") ||
                packageName.startsWith("com.sonyericsson.home") ||
                packageName.startsWith("com.nothing.launcher") ||
                packageName == "com.google.android.apps.nexuslauncher" ||  // Pixel Launcher
                packageName == "com.teslacoilsw.launcher" ||               // Nova Launcher
                packageName == "com.microsoft.launcher" ||                 // Microsoft Launcher
                packageName == "com.niagara.launcher" ||                   // Niagara Launcher
                packageName == "com.actionlauncher.playstore" ||           // Action Launcher
                packageName == "com.smartlauncher.nexus" ||                // Smart Launcher
                packageName == "bitpit.launcher"                           // AIO Launcher
    }

    private fun handleAppExit(packageName: String?, now: Long) {
        if (packageName == null) return
        if (!preferencesManager.isAppConfigured(packageName)) return

        // Sauvegarder le temps de sortie pour le debounce (réentrées rapides)
        recentExitTimes[packageName] = now

        // Mettre à jour le temps de sortie
        preferencesManager.setAppExitTime(packageName, now)

        // Arrêter le timer périodique
        PeriodicTimerManager.stopTimer(applicationContext, packageName)
    }

    private fun startPeriodicTimerIfNeeded(packageName: String) {
        val timerSeconds = appRepository.getAppPeriodicTimer(packageName)
        if (timerSeconds > 0 && !PeriodicTimerManager.isTimerActive(packageName)) {
            PeriodicTimerManager.startTimer(applicationContext, packageName, timerSeconds)
        }
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        super.onDestroy()
        PeriodicTimerManager.stopAllTimers(applicationContext)
    }
}
