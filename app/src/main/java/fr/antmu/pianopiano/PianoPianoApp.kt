package fr.antmu.pianopiano

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.data.repository.AppRepository
import fr.antmu.pianopiano.data.repository.SettingsRepository

class PianoPianoApp : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set
    lateinit var appRepository: AppRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferencesManager = PreferencesManager(this)
        appRepository = AppRepository(this)
        settingsRepository = SettingsRepository(this)

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "pianopiano_pause_channel"
        const val NOTIFICATION_ID = 1001

        lateinit var instance: PianoPianoApp
            private set
    }
}
