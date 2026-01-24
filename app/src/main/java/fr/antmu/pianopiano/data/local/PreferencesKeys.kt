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
    const val KEY_ACTIVE_TIMER_PACKAGE = "active_timer_package"
    const val EXIT_THRESHOLD_MS = 300000L  // 5 minutes

    // Onboarding
    const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    const val KEY_LAST_ONBOARDING_VERSION = "last_onboarding_version"

    // Daily peanuts tracking
    const val KEY_PEANUTS_TODAY = "peanuts_today"
    const val KEY_PEANUTS_TODAY_DATE = "peanuts_today_date"

    // Default values
    const val DEFAULT_PAUSE_DURATION = 12
    const val DEFAULT_PEANUT_COUNT = 0
    const val DEFAULT_SERVICE_ENABLED = false
}
