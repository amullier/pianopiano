package fr.antmu.pianopiano.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.data.repository.AppRepository

class AppLaunchDetectorService : AccessibilityService() {

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
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val newPkg = event.packageName?.toString() ?: return
        val now = System.currentTimeMillis()

        // 🔒 Transition interne → IGNORER COMPLÈTEMENT
        if (newPkg == currentForegroundPackage) {
            return
        }

        // 👉 C'est un vrai changement d'application
        val previousPkg = currentForegroundPackage

        // 📱 Package système (launcher, systemui, notre app)
        if (isSystemPackage(newPkg)) {
            currentForegroundPackage = null
            handleAppExit(previousPkg, now)
            return
        }

        currentForegroundPackage = newPkg

        // 1️⃣ Gérer la sortie de l'app précédente
        handleAppExit(previousPkg, now)

        // 2️⃣ Si app non configurée → rien
        if (!preferencesManager.isAppConfigured(newPkg)) {
            return
        }

        // 3️⃣ Décider si pause initiale
        val lastEnterTime = preferencesManager.getAppEnterTime(newPkg)
        val lastExitTime = preferencesManager.getAppExitTime(newPkg)

        val shouldInitialPause = when {
            lastEnterTime == 0L -> true                    // Première fois
            lastExitTime == 0L -> true                     // Forcé (après Annuler)
            (now - lastExitTime) > 5_000 -> true           // Plus de 5s depuis sortie
            else -> false
        }

        // 4️⃣ Mettre à jour le temps d'entrée
        preferencesManager.setAppEnterTime(newPkg, now)

        // 5️⃣ Afficher pause si nécessaire
        if (shouldInitialPause) {
            ServiceHelper.startPauseOverlay(applicationContext, newPkg, isPeriodic = false)
        } else {
            // Pas de pause initiale, démarrer le timer périodique si configuré
            startPeriodicTimerIfNeeded(newPkg)
        }
    }

    private fun isSystemPackage(packageName: String): Boolean {
        return packageName == "fr.antmu.pianopiano" ||
                packageName == "com.android.systemui" ||
                packageName.startsWith("com.android.launcher") ||
                packageName.startsWith("com.google.android.launcher") ||
                packageName.startsWith("com.sec.android.app.launcher") ||
                packageName.startsWith("com.miui.home") ||
                packageName.startsWith("com.huawei.android.launcher")
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
