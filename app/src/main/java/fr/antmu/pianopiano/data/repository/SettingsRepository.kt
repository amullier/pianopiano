package fr.antmu.pianopiano.data.repository

import android.content.Context
import fr.antmu.pianopiano.data.local.PreferencesManager

class SettingsRepository(context: Context) {

    private val preferencesManager = PreferencesManager(context)

    var pauseDuration: Int
        get() = preferencesManager.pauseDuration
        set(value) {
            preferencesManager.pauseDuration = value
        }

    var peanutCount: Int
        get() = preferencesManager.peanutCount
        set(value) {
            preferencesManager.peanutCount = value
        }

    val peanutsToday: Int
        get() = preferencesManager.peanutsToday

    var serviceEnabled: Boolean
        get() = preferencesManager.serviceEnabled
        set(value) {
            preferencesManager.serviceEnabled = value
        }

    fun incrementPeanuts() {
        preferencesManager.incrementPeanuts()
    }

    companion object {
        val PAUSE_DURATION_VALUES = listOf(6, 12, 18, 24, 30)
    }
}
