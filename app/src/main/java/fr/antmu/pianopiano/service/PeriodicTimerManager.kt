package fr.antmu.pianopiano.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.data.repository.AppRepository

object PeriodicTimerManager {

    private val handler = Handler(Looper.getMainLooper())
    private val activeTimers = mutableMapOf<String, Runnable>()

    fun startTimer(context: Context, packageName: String, intervalSeconds: Int) {
        stopTimer(context, packageName)

        if (intervalSeconds <= 0) return

        val preferencesManager = PreferencesManager(context)
        preferencesManager.activeTimerPackage = packageName

        val runnable = object : Runnable {
            override fun run() {
                AppLaunchDetectorService.activePauseForPackage = packageName
                ServiceHelper.startPeriodicPause(context, packageName)
                handler.postDelayed(this, intervalSeconds * 1000L)
            }
        }

        activeTimers[packageName] = runnable
        handler.postDelayed(runnable, intervalSeconds * 1000L)
    }

    fun stopTimer(context: Context, packageName: String) {
        activeTimers[packageName]?.let { runnable ->
            handler.removeCallbacks(runnable)
            activeTimers.remove(packageName)
        }
        val preferencesManager = PreferencesManager(context)
        if (preferencesManager.activeTimerPackage == packageName) {
            preferencesManager.activeTimerPackage = null
        }
    }

    fun stopAllTimers(context: Context) {
        activeTimers.forEach { (_, runnable) ->
            handler.removeCallbacks(runnable)
        }
        activeTimers.clear()
        val preferencesManager = PreferencesManager(context)
        preferencesManager.activeTimerPackage = null
    }

    fun isTimerActive(packageName: String): Boolean {
        return activeTimers.containsKey(packageName)
    }

    fun restoreTimerIfNeeded(context: Context) {
        val preferencesManager = PreferencesManager(context)
        val activePackage = preferencesManager.activeTimerPackage ?: return

        val appRepository = AppRepository(context)
        val timerSeconds = appRepository.getAppPeriodicTimer(activePackage)
        if (timerSeconds <= 0) {
            preferencesManager.activeTimerPackage = null
            return
        }

        // Vérifier si on est dans la fenêtre de 5 secondes
        val lastExitTime = preferencesManager.getAppExitTime(activePackage)
        val timeSinceExit = System.currentTimeMillis() - lastExitTime
        if (lastExitTime == 0L || timeSinceExit > 10_000) {
            preferencesManager.activeTimerPackage = null
            return
        }

        // Restaurer le timer
        startTimer(context, activePackage, timerSeconds)
    }

    fun onInitialPauseFinished(context: Context, packageName: String) {
        val appRepository = AppRepository(context)
        val timerSeconds = appRepository.getAppPeriodicTimer(packageName)
        if (timerSeconds > 0) {
            startTimer(context, packageName, timerSeconds)
        }
    }

    fun onPeriodicPauseFinished(context: Context, packageName: String) {
        // Timer déjà relancé dans le Runnable, rien à faire
    }
}
