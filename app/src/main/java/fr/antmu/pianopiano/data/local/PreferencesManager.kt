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
    }

    // --- Periodic Timer Methods ---

    private fun getLastActiveTimestamps(): MutableMap<String, Long> {
        val json = prefs.getString(PreferencesKeys.KEY_LAST_ACTIVE_TIMESTAMPS, null) ?: return mutableMapOf()
        val type = object : TypeToken<MutableMap<String, Long>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun saveLastActiveTimestamps(timestamps: Map<String, Long>) {
        val json = gson.toJson(timestamps)
        prefs.edit().putString(PreferencesKeys.KEY_LAST_ACTIVE_TIMESTAMPS, json).apply()
    }

    fun getLastActiveTimestamp(packageName: String): Long {
        return getLastActiveTimestamps()[packageName] ?: 0L
    }

    fun setLastActiveTimestamp(packageName: String, timestamp: Long) {
        val timestamps = getLastActiveTimestamps()
        timestamps[packageName] = timestamp
        saveLastActiveTimestamps(timestamps)
    }

    fun shouldResetTimer(packageName: String): Boolean {
        val lastActive = getLastActiveTimestamp(packageName)
        if (lastActive == 0L) return true
        return System.currentTimeMillis() - lastActive > PreferencesKeys.EXIT_THRESHOLD_MS
    }

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
}
