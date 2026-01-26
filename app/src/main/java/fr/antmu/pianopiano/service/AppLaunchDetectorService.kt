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

        // 📱 Package système (launcher, systemui, notre app)
        val isSystem = isSystemPackage(newPkg)
        Log.d(TAG, "🔍 isSystemPackage($newPkg) = $isSystem")
        if (isSystem) {
            Log.d(TAG, "📱 Package système détecté, on ignore")
            //
//            currentForegroundPackage = null
//            handleAppExit(previousPkg, now)
            return
        }
        Log.d(TAG, "✅ Package non-système, on continue le traitement")

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

    private fun isSystemPackage(packageName: String): Boolean {
        return packageName == "fr.antmu.pianopiano" ||
                packageName == "com.android.systemui" ||
                packageName.startsWith("com.android.launcher") ||
                packageName.startsWith("com.google.android.launcher") ||
                packageName.startsWith("com.sec.android.app.launcher") ||
                packageName.startsWith("com.miui.home") ||
                packageName.startsWith("com.huawei.android.launcher") ||
                // Claviers - ne pas considérer comme changement d'app
                packageName.contains("inputmethod") ||
                packageName.contains("keyboard") ||
                packageName == "com.google.android.inputmethod.latin" ||  // Gboard
                packageName == "com.samsung.android.honeyboard" ||         // Samsung Keyboard
                packageName == "com.touchtype.swiftkey" ||                 // SwiftKey
                packageName == "com.sec.android.inputmethod"               // Samsung ancien clavier
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
