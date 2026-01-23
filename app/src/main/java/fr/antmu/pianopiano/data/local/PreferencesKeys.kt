package fr.antmu.pianopiano.data.local

object PreferencesKeys {
    const val PREFS_NAME = "pianopiano_prefs"

    // App settings
    const val KEY_PAUSE_DURATION = "pause_duration"
    const val KEY_PEANUT_COUNT = "peanut_count"
    const val KEY_SERVICE_ENABLED = "service_enabled"

    // Configured apps (stored as JSON)
    const val KEY_CONFIGURED_APPS = "configured_apps"

    // Periodic timer
    const val KEY_LAST_ACTIVE_TIMESTAMPS = "last_active_timestamps"
    const val EXIT_THRESHOLD_MS = 300000L  // 5 minutes

    // Default values
    const val DEFAULT_PAUSE_DURATION = 12
    const val DEFAULT_PEANUT_COUNT = 0
    const val DEFAULT_SERVICE_ENABLED = false
}
