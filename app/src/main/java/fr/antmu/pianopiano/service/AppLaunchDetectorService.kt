package fr.antmu.pianopiano.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.data.repository.AppRepository

class AppLaunchDetectorService : AccessibilityService() {

    companion object {
        private const val TAG = "AppLaunchDetector"
    }

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var appRepository: AppRepository

    private var currentForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        preferencesManager = PreferencesManager(applicationContext)
        appRepository = AppRepository(applicationContext)
        PeriodicTimerManager.restoreTimerIfNeeded(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "onAccessibilityEvent() appelé")
        Log.d(TAG, "Event reçu: $event")
        Log.d(TAG, "Event type: ${event?.eventType}")
        Log.d(TAG, "Event type name: ${event?.eventType?.let { AccessibilityEvent.eventTypeToString(it) }}")
        Log.d(TAG, "Package name: ${event?.packageName}")
        Log.d(TAG, "Class name: ${event?.className}")
        Log.d(TAG, "Content description: ${event?.contentDescription}")
        Log.d(TAG, "Text: ${event?.text}")
        Log.d(TAG, "Source: ${event?.source}")

        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "❌ Event ignoré: pas TYPE_WINDOW_STATE_CHANGED (type=${event?.eventType})")
            return
        }
        Log.d(TAG, "✅ Event TYPE_WINDOW_STATE_CHANGED accepté")

        val newPkg = event.packageName?.toString()
        if (newPkg == null) {
            Log.d(TAG, "❌ Package name est null, on ignore")
            return
        }
        Log.d(TAG, "📦 Nouveau package: $newPkg")

        val now = System.currentTimeMillis()
        Log.d(TAG, "⏱️ Timestamp actuel: $now")
        Log.d(TAG, "📍 currentForegroundPackage: $currentForegroundPackage")

        // 🔒 Transition interne → IGNORER COMPLÈTEMENT
        if (newPkg == currentForegroundPackage) {
            Log.d(TAG, "🔒 Transition interne détectée (même package), on ignore")
            return
        }
        Log.d(TAG, "✅ Changement d'app détecté: $currentForegroundPackage → $newPkg")

        // 👉 C'est un vrai changement d'application
        val previousPkg = currentForegroundPackage
        Log.d(TAG, "📤 Package précédent: $previousPkg")

        // 🎹 Overlay temporaire (clavier, systemui, notre app) → ignorer complètement
        if (isTemporaryOverlay(newPkg)) {
            Log.d(TAG, "🎹 Overlay temporaire détecté ($newPkg), on ignore complètement")
            return
        }

        // 🏠 Launcher/Home → traiter comme sortie d'app
        if (isLauncher(newPkg)) {
            Log.d(TAG, "🏠 Launcher détecté ($newPkg), traitement comme sortie d'app")
            currentForegroundPackage = null
            handleAppExit(previousPkg, now)
            return
        }
        Log.d(TAG, "✅ App normale détectée, on continue le traitement")

        currentForegroundPackage = newPkg
        Log.d(TAG, "📍 currentForegroundPackage mis à jour: $currentForegroundPackage")

        // 1️⃣ Gérer la sortie de l'app précédente
        Log.d(TAG, "1️⃣ Appel handleAppExit($previousPkg, $now)")
        handleAppExit(previousPkg, now)

        // 2️⃣ Si app non configurée → rien
        val isConfigured = preferencesManager.isAppConfigured(newPkg)
        Log.d(TAG, "2️⃣ isAppConfigured($newPkg) = $isConfigured")
        if (!isConfigured) {
            Log.d(TAG, "❌ App non configurée, on s'arrête là")
            return
        }
        Log.d(TAG, "✅ App configurée, on continue")

        // 3️⃣ Décider si pause initiale
        val lastEnterTime = preferencesManager.getAppEnterTime(newPkg)
        val lastExitTime = preferencesManager.getAppExitTime(newPkg)
        Log.d(TAG, "3️⃣ Récupération des temps:")
        Log.d(TAG, "   lastEnterTime: $lastEnterTime")
        Log.d(TAG, "   lastExitTime: $lastExitTime")
        Log.d(TAG, "   now: $now")
        Log.d(TAG, "   (now - lastExitTime): ${now - lastExitTime}ms")

        val shouldInitialPause = when {
            lastEnterTime == 0L -> {
                Log.d(TAG, "   → shouldInitialPause=true (lastEnterTime == 0L, première fois)")
                true
            }
            lastExitTime == 0L -> {
                Log.d(TAG, "   → shouldInitialPause=true (lastExitTime == 0L, forcé après Annuler)")
                true
            }
            (now - lastExitTime) > 5_000 -> {
                Log.d(TAG, "   → shouldInitialPause=true ((now - lastExitTime) > 5000ms)")
                true
            }
            else -> {
                Log.d(TAG, "   → shouldInitialPause=false (retour rapide dans l'app)")
                false
            }
        }
        Log.d(TAG, "   Décision finale: shouldInitialPause = $shouldInitialPause")

        // 4️⃣ Mettre à jour le temps d'entrée
        Log.d(TAG, "4️⃣ setAppEnterTime($newPkg, $now)")
        preferencesManager.setAppEnterTime(newPkg, now)

        // 5️⃣ Afficher pause si nécessaire
        Log.d(TAG, "5️⃣ Action finale:")
        if (shouldInitialPause) {
            Log.d(TAG, "   🎯 Démarrage PauseOverlay (isPeriodic=false)")
            ServiceHelper.startPauseOverlay(applicationContext, newPkg, isPeriodic = false)
        } else {
            Log.d(TAG, "   ⏰ Pas de pause initiale, vérification timer périodique")
            // Pas de pause initiale, démarrer le timer périodique si configuré
            startPeriodicTimerIfNeeded(newPkg)
        }
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /**
     * Overlays temporaires à ignorer complètement (pas de pause, pas de changement d'état)
     */
    private fun isTemporaryOverlay(packageName: String): Boolean {
        return packageName == "fr.antmu.pianopiano" ||
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
    }

    /**
     * Launchers/Home = sortie d'app (déclenche handleAppExit)
     */
    private fun isLauncher(packageName: String): Boolean {
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
