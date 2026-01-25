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
        // Restaurer le timer périodique si l'app a été tuée
        PeriodicTimerManager.restoreTimerIfNeeded(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = extractPackageName(event) ?: return

        logEventReceived(packageName)

        if (isSystemOrLauncherPackage(packageName)) {
            handleSystemPackageDetected()
            return
        }

        // Détecter les transitions internes AVANT de mettre à jour previousForegroundPackage
        val isInternalTransition = previousForegroundPackage == packageName

        updateForegroundPackages(packageName)

        if (!isConfiguredApp(packageName)) {
            logAppNotConfigured(packageName)
            return
        }

        // Transitions internes (changement d'onglet, etc.) : PAS de pause initiale
        if (isInternalTransition) {
            handleInternalTransition(packageName)
            return
        }

        if (shouldSkipPauseCheck(packageName)) {
            return
        }

        decidePauseAction(packageName)
    }

    // ==================== Extraction et validation ====================

    private fun extractPackageName(event: AccessibilityEvent?): String? {
        if (event == null) return null
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return null
        return event.packageName?.toString()
    }

    private fun logEventReceived(packageName: String) {
        android.util.Log.d("AppLaunchDetector", "Event: $packageName (previous=$previousForegroundPackage)")
    }

    private fun logAppNotConfigured(packageName: String) {
        android.util.Log.d("AppLaunchDetector", "[$packageName] App NON configurée → skip")
    }

    // ==================== Détection des packages système ====================

    private fun isSystemOrLauncherPackage(packageName: String): Boolean {
        return packageName == "fr.antmu.pianopiano" ||
                packageName == "com.android.systemui" ||
                packageName.startsWith("com.android.launcher")
    }

    private fun handleSystemPackageDetected() {
        android.util.Log.d("AppLaunchDetector", "→ Système/Launcher détecté")
        // L'utilisateur a quitté l'app pour aller sur le launcher/système
        // On met à jour le timestamp AU MOMENT où l'utilisateur PART de l'app
        handleAppLeft(previousForegroundPackage)
        PeriodicTimerManager.setCurrentForegroundPackage(null)
    }

    // ==================== Gestion de l'état des packages au premier plan ====================

    private fun updateForegroundPackages(packageName: String) {
        PeriodicTimerManager.setCurrentForegroundPackage(packageName)

        // Si l'utilisateur a changé d'app, on enregistre qu'il a quitté l'ancienne
        if (previousForegroundPackage != null && previousForegroundPackage != packageName) {
            handleAppLeft(previousForegroundPackage)
        }

        previousForegroundPackage = packageName
    }

    // ==================== Vérification de la configuration ====================

    private fun isConfiguredApp(packageName: String): Boolean {
        return preferencesManager.isAppConfigured(packageName)
    }

    // ==================== Vérifications pour skip ====================

    private fun shouldSkipPauseCheck(packageName: String): Boolean {
        if (isPackageExempted(packageName)) {
            return true
        }

        if (isCooldownActive(packageName)) {
            return true
        }

        // Pas de skip, on continue
        return false
    }

    private fun isPackageExempted(packageName: String): Boolean {
        if (ServiceHelper.isPackageExempted(packageName)) {
            android.util.Log.d("AppLaunchDetector", "[$packageName] Package exempté → skip")
            // Mettre à jour le timestamp pour suivre l'activité
            appRepository.setLastActiveTimestamp(packageName, System.currentTimeMillis())
            return true
        }
        return false
    }

    private fun isCooldownActive(packageName: String): Boolean {
        val currentTime = System.currentTimeMillis()
        if (packageName == lastDetectedPackage &&
            currentTime - lastDetectionTime < DETECTION_COOLDOWN_MS
        ) {
            android.util.Log.d("AppLaunchDetector", "[$packageName] Cooldown actif → skip")
            return true
        }

        // Mettre à jour le dernier package détecté
        lastDetectedPackage = packageName
        lastDetectionTime = currentTime
        return false
    }

    // ==================== Décision de pause ====================

    private fun decidePauseAction(packageName: String) {
        val timeSinceLeft = calculateTimeSinceLeft(packageName)

        android.util.Log.d("AppLaunchDetector",
            "[$packageName] Temps depuis départ: ${timeSinceLeft.elapsedMs}ms, shouldReset=${timeSinceLeft.shouldReset}")

        if (timeSinceLeft.shouldReset) {
            showInitialPause(packageName)
        } else {
            resumeWithoutPause(packageName)
        }
    }

    private data class TimeSinceLeft(
        val elapsedMs: Long,
        val shouldReset: Boolean
    )

    private fun calculateTimeSinceLeft(packageName: String): TimeSinceLeft {
        val lastActive = appRepository.getLastActiveTimestamp(packageName)
        val currentTime = System.currentTimeMillis()
        val elapsed = if (lastActive == 0L) -1L else currentTime - lastActive
        val shouldReset = appRepository.shouldResetTimer(packageName)

        return TimeSinceLeft(elapsed, shouldReset)
    }

    private fun showInitialPause(packageName: String) {
        android.util.Log.d("AppLaunchDetector", "[$packageName] 🔴 PAUSE INITIALE")
        ServiceHelper.startPauseOverlay(applicationContext, packageName, isPeriodic = false)
    }

    private fun resumeWithoutPause(packageName: String) {
        android.util.Log.d("AppLaunchDetector", "[$packageName] ✅ PAS DE PAUSE (tolérance 5s)")

        val currentTime = System.currentTimeMillis()
        appRepository.setLastActiveTimestamp(packageName, currentTime)

        startPeriodicTimerIfNeeded(packageName)
    }

    private fun startPeriodicTimerIfNeeded(packageName: String) {
        val timerSeconds = appRepository.getAppPeriodicTimer(packageName)
        if (timerSeconds > 0 && !PeriodicTimerManager.isTimerActive(packageName)) {
            android.util.Log.d("AppLaunchDetector", "[$packageName] ⏱️  Démarrage timer périodique ($timerSeconds secondes)")
            PeriodicTimerManager.startTimer(applicationContext, packageName, timerSeconds)
        }
    }

    // ==================== Gestion des transitions ====================

    private fun handleInternalTransition(packageName: String) {
        android.util.Log.d("AppLaunchDetector", "[$packageName] 🔄 Transition interne détectée → PAS de pause")
        // Mettre à jour le timestamp pour le tracking
        appRepository.setLastActiveTimestamp(packageName, System.currentTimeMillis())
        // Démarrer le timer périodique si nécessaire (et pas déjà actif)
        startPeriodicTimerIfNeeded(packageName)
    }

    private fun handleAppLeft(packageName: String?) {
        if (packageName == null) return
        if (preferencesManager.isAppConfigured(packageName)) {
            android.util.Log.d("AppLaunchDetector", "[$packageName] handleAppLeft - MAJ timestamp")
            // Mettre à jour le timestamp quand l'utilisateur quitte
            PeriodicTimerManager.updateLastActiveTime(applicationContext, packageName)
            PeriodicTimerManager.onAppLeft(applicationContext, packageName)
        }
    }

    override fun onInterrupt() {
        // Required override, nothing to do
    }

    override fun onDestroy() {
        super.onDestroy()
        PeriodicTimerManager.stopAllTimers(applicationContext)
        // Plus besoin de stopPauseOverlay - l'Activity se gère elle-même
    }
}
