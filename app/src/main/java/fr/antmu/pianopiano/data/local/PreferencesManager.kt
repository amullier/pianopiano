package fr.antmu.pianopiano.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fr.antmu.pianopiano.data.model.ConfiguredApp

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PreferencesKeys.PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    var pauseDuration: Int
        get() = prefs.getInt(PreferencesKeys.KEY_PAUSE_DURATION, PreferencesKeys.DEFAULT_PAUSE_DURATION)
        set(value) = prefs.edit().putInt(PreferencesKeys.KEY_PAUSE_DURATION, value).apply()

    var peanutCount: Int
        get() = prefs.getInt(PreferencesKeys.KEY_PEANUT_COUNT, PreferencesKeys.DEFAULT_PEANUT_COUNT)
        set(value) = prefs.edit().putInt(PreferencesKeys.KEY_PEANUT_COUNT, value).apply()

    var serviceEnabled: Boolean
        get() = prefs.getBoolean(PreferencesKeys.KEY_SERVICE_ENABLED, PreferencesKeys.DEFAULT_SERVICE_ENABLED)
        set(value) = prefs.edit().putBoolean(PreferencesKeys.KEY_SERVICE_ENABLED, value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(PreferencesKeys.KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(PreferencesKeys.KEY_ONBOARDING_COMPLETED, value).apply()

    var lastOnboardingVersion: Int
        get() = prefs.getInt(PreferencesKeys.KEY_LAST_ONBOARDING_VERSION, 0)
        set(value) = prefs.edit().putInt(PreferencesKeys.KEY_LAST_ONBOARDING_VERSION, value).apply()

    var tutorialCompleted: Boolean
        get() = prefs.getBoolean(PreferencesKeys.KEY_TUTORIAL_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(PreferencesKeys.KEY_TUTORIAL_COMPLETED, value).apply()

    fun getConfiguredApps(): List<ConfiguredApp> {
        val json = prefs.getString(PreferencesKeys.KEY_CONFIGURED_APPS, null) ?: return emptyList()
        val type = object : TypeToken<List<ConfiguredApp>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveConfiguredApps(apps: List<ConfiguredApp>) {
        val json = gson.toJson(apps)
        prefs.edit().putString(PreferencesKeys.KEY_CONFIGURED_APPS, json).apply()
    }

    fun addConfiguredApp(app: ConfiguredApp) {
        val apps = getConfiguredApps().toMutableList()
        val existingIndex = apps.indexOfFirst { it.packageName == app.packageName }
        if (existingIndex >= 0) {
            apps[existingIndex] = app
        } else {
            apps.add(app)
        }
        saveConfiguredApps(apps)
    }

    fun removeConfiguredApp(packageName: String) {
        val apps = getConfiguredApps().filter { it.packageName != packageName }
        saveConfiguredApps(apps)
    }

    fun isAppConfigured(packageName: String): Boolean {
        return getConfiguredApps().any { it.packageName == packageName && it.isEnabled }
    }

    fun incrementPeanuts() {
        peanutCount++
        incrementPeanutsToday()
        updatePeanutsHistory()
    }

    // --- Daily Peanuts Tracking ---

    private fun getTodayDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        return "${calendar.get(java.util.Calendar.YEAR)}-${calendar.get(java.util.Calendar.MONTH)}-${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
    }

    private fun getDateString(calendar: java.util.Calendar): String {
        return "${calendar.get(java.util.Calendar.YEAR)}-${calendar.get(java.util.Calendar.MONTH)}-${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
    }

    val peanutsToday: Int
        get() {
            val savedDate = prefs.getString(PreferencesKeys.KEY_PEANUTS_TODAY_DATE, null)
            val today = getTodayDateString()
            return if (savedDate == today) {
                prefs.getInt(PreferencesKeys.KEY_PEANUTS_TODAY, 0)
            } else {
                0
            }
        }

    private fun incrementPeanutsToday() {
        val today = getTodayDateString()
        val savedDate = prefs.getString(PreferencesKeys.KEY_PEANUTS_TODAY_DATE, null)

        val currentCount = if (savedDate == today) {
            prefs.getInt(PreferencesKeys.KEY_PEANUTS_TODAY, 0)
        } else {
            0
        }

        prefs.edit()
            .putString(PreferencesKeys.KEY_PEANUTS_TODAY_DATE, today)
            .putInt(PreferencesKeys.KEY_PEANUTS_TODAY, currentCount + 1)
            .apply()
    }

    private fun getPeanutsHistory(): MutableMap<String, Int> {
        val json = prefs.getString(PreferencesKeys.KEY_PEANUTS_HISTORY, null) ?: return mutableMapOf()
        val type = object : TypeToken<MutableMap<String, Int>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun savePeanutsHistory(history: Map<String, Int>) {
        val json = gson.toJson(history)
        prefs.edit().putString(PreferencesKeys.KEY_PEANUTS_HISTORY, json).apply()
    }

    private fun updatePeanutsHistory() {
        val history = getPeanutsHistory()
        val today = getTodayDateString()

        // Incrémenter le compteur d'aujourd'hui
        history[today] = (history[today] ?: 0) + 1

        // Nettoyer les entrées de plus de 30 jours pour ne pas surcharger
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -30)
        val cutoffDate = getDateString(calendar)

        val cleanedHistory = history.filterKeys { date ->
            date >= cutoffDate
        }

        savePeanutsHistory(cleanedHistory)
    }

    val peanutsLast7Days: Int
        get() {
            val history = getPeanutsHistory()
            val calendar = java.util.Calendar.getInstance()
            val today = getTodayDateString()
            var total = 0

            // Compter les peanuts des 7 derniers jours (y compris aujourd'hui)
            for (i in 0..6) {
                val dateString = getDateString(calendar)

                // Pour aujourd'hui, utiliser peanutsToday au lieu de l'historique
                // (au cas où l'historique n'est pas encore synchronisé)
                if (dateString == today) {
                    total += peanutsToday
                } else {
                    total += history[dateString] ?: 0
                }

                calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
            }

            return total
        }

    // --- Periodic Timer Methods ---

    fun getAppPeriodicTimer(packageName: String): Int {
        val apps = getConfiguredApps()
        return apps.find { it.packageName == packageName }?.periodicTimerSeconds ?: 0
    }

    fun setAppPeriodicTimer(packageName: String, seconds: Int) {
        val apps = getConfiguredApps().toMutableList()
        val index = apps.indexOfFirst { it.packageName == packageName }
        if (index >= 0) {
            apps[index] = apps[index].copy(periodicTimerSeconds = seconds)
            saveConfiguredApps(apps)
        }
    }

    // --- Active Timer Persistence ---

    var activeTimerPackage: String?
        get() = prefs.getString(PreferencesKeys.KEY_ACTIVE_TIMER_PACKAGE, null)
        set(value) {
            if (value == null) {
                prefs.edit().remove(PreferencesKeys.KEY_ACTIVE_TIMER_PACKAGE).apply()
            } else {
                prefs.edit().putString(PreferencesKeys.KEY_ACTIVE_TIMER_PACKAGE, value).apply()
            }
        }

    // --- App Enter/Exit Times ---

    private fun getAppEnterTimes(): MutableMap<String, Long> {
        val json = prefs.getString(PreferencesKeys.KEY_APP_ENTER_TIMES, null) ?: return mutableMapOf()
        val type = object : TypeToken<MutableMap<String, Long>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun saveAppEnterTimes(times: Map<String, Long>) {
        val json = gson.toJson(times)
        prefs.edit().putString(PreferencesKeys.KEY_APP_ENTER_TIMES, json).apply()
    }

    fun getAppEnterTime(packageName: String): Long {
        return getAppEnterTimes()[packageName] ?: 0L
    }

    fun setAppEnterTime(packageName: String, timestamp: Long) {
        val times = getAppEnterTimes()
        times[packageName] = timestamp
        saveAppEnterTimes(times)
    }

    private fun getAppExitTimes(): MutableMap<String, Long> {
        val json = prefs.getString(PreferencesKeys.KEY_APP_EXIT_TIMES, null) ?: return mutableMapOf()
        val type = object : TypeToken<MutableMap<String, Long>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun saveAppExitTimes(times: Map<String, Long>) {
        val json = gson.toJson(times)
        prefs.edit().putString(PreferencesKeys.KEY_APP_EXIT_TIMES, json).apply()
    }

    fun getAppExitTime(packageName: String): Long {
        return getAppExitTimes()[packageName] ?: 0L
    }

    fun setAppExitTime(packageName: String, timestamp: Long) {
        val times = getAppExitTimes()
        times[packageName] = timestamp
        saveAppExitTimes(times)
    }

    // --- Force Next Pause Flag ---

    fun setForceNextPause(packageName: String, force: Boolean) {
        prefs.edit()
            .putBoolean("force_next_pause_$packageName", force)
            .apply()
    }

    fun shouldForceNextPause(packageName: String): Boolean {
        return prefs.getBoolean("force_next_pause_$packageName", false)
    }
}
