package fr.antmu.pianopiano.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import fr.antmu.pianopiano.data.local.PreferencesManager

class AppLaunchDetectorService : AccessibilityService() {

    private lateinit var preferencesManager: PreferencesManager
    private var lastDetectedPackage: String? = null
    private var lastDetectionTime: Long = 0

    companion object {
        private const val DETECTION_COOLDOWN_MS = 2000L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        preferencesManager = PreferencesManager(applicationContext)
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
            return
        }

        // Ignore if overlay is already showing
        if (ServiceHelper.isOverlayShowing()) {
            return
        }

        // Ignore if package is currently exempted (user just clicked Cancel or Continue)
        if (ServiceHelper.isPackageExempted(packageName)) {
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
            ServiceHelper.startPauseOverlay(applicationContext, packageName)
        }
    }

    override fun onInterrupt() {
        // Required override, nothing to do
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceHelper.stopPauseOverlay(applicationContext)
    }
}
