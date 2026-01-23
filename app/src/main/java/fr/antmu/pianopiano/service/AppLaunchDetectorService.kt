package fr.antmu.pianopiano.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.data.repository.AppRepository

class AppLaunchDetectorService : AccessibilityService() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var appRepository: AppRepository
    private var lastDetectedPackage: String? = null
    private var lastDetectionTime: Long = 0
    private var previousForegroundPackage: String? = null

    companion object {
        private const val DETECTION_COOLDOWN_MS = 2000L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        preferencesManager = PreferencesManager(applicationContext)
        appRepository = AppRepository(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Ignore our own app and system UI
        if (packageName == "fr.antmu.pianopiano" ||
            packageName == "com.android.systemui" ||
            packageName.startsWith("com.android.launcher")
        ) {
            // L'utilisateur a quitté l'app pour aller sur le launcher/système
            handleAppLeft(previousForegroundPackage)
            previousForegroundPackage = null
            PeriodicTimerManager.setCurrentForegroundPackage(null)
            return
        }

        // Mettre à jour le package au premier plan
        PeriodicTimerManager.setCurrentForegroundPackage(packageName)

        // Détecter si l'utilisateur a changé d'app configurée
        if (previousForegroundPackage != null && previousForegroundPackage != packageName) {
            handleAppLeft(previousForegroundPackage)
        }

        previousForegroundPackage = packageName

        // Ignore if overlay is already showing
        if (ServiceHelper.isOverlayShowing()) {
            return
        }

        // Ignore if package is currently exempted (user just clicked Cancel or Continue)
        if (ServiceHelper.isPackageExempted(packageName)) {
            // Mettre à jour le timestamp d'activité si l'app a un timer périodique actif
            if (PeriodicTimerManager.isTimerActive(packageName)) {
                PeriodicTimerManager.updateLastActiveTime(applicationContext, packageName)
            }
            return
        }

        // Apply cooldown to prevent multiple triggers
        val currentTime = System.currentTimeMillis()
        if (packageName == lastDetectedPackage &&
            currentTime - lastDetectionTime < DETECTION_COOLDOWN_MS
        ) {
            return
        }

        // Check if this app is configured for pause
        if (preferencesManager.isAppConfigured(packageName)) {
            lastDetectedPackage = packageName
            lastDetectionTime = currentTime

            // Vérifier si on doit réinitialiser le timer (absent > 5 min)
            val shouldReset = appRepository.shouldResetTimer(packageName)

            if (shouldReset) {
                // Première visite ou retour après > 5 min : pause initiale
                ServiceHelper.startPauseOverlay(applicationContext, packageName)
            } else {
                // Retour dans les 5 min : reprendre le timer si configuré
                val timerSeconds = appRepository.getAppPeriodicTimer(packageName)
                if (timerSeconds > 0 && !PeriodicTimerManager.isTimerActive(packageName)) {
                    // Relancer le timer périodique
                    PeriodicTimerManager.startTimer(applicationContext, packageName, timerSeconds)
                    PeriodicTimerManager.updateLastActiveTime(applicationContext, packageName)
                }
            }
        }
    }

    private fun handleAppLeft(packageName: String?) {
        if (packageName == null) return
        if (preferencesManager.isAppConfigured(packageName)) {
            // Mettre à jour le timestamp quand l'utilisateur quitte
            PeriodicTimerManager.updateLastActiveTime(applicationContext, packageName)
            PeriodicTimerManager.onAppLeft(packageName)
        }
    }

    override fun onInterrupt() {
        // Required override, nothing to do
    }

    override fun onDestroy() {
        super.onDestroy()
        PeriodicTimerManager.stopAllTimers()
        ServiceHelper.stopPauseOverlay(applicationContext)
    }
}
