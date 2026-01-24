package fr.antmu.pianopiano.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.data.repository.AppRepository

object PeriodicTimerManager {

    private val handler = Handler(Looper.getMainLooper())
    private val activeTimers = mutableMapOf<String, Runnable>()
    private var currentForegroundPackage: String? = null

    fun startTimer(context: Context, packageName: String, intervalSeconds: Int) {
        // Arrêter le timer existant s'il y en a un
        stopTimer(context, packageName)

        if (intervalSeconds <= 0) return

        // Sauvegarder le package avec timer actif
        val preferencesManager = PreferencesManager(context)
        preferencesManager.activeTimerPackage = packageName

        val runnable = object : Runnable {
            override fun run() {
                // Vérifier si l'app est toujours au premier plan
                if (currentForegroundPackage == packageName && !ServiceHelper.isOverlayShowing()) {
                    // Déclencher la pause périodique
                    startPeriodicPause(context, packageName)
                }
                // Relancer le timer
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
        // Effacer le package actif si c'est celui qu'on arrête
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
        // Effacer le package actif
        val preferencesManager = PreferencesManager(context)
        preferencesManager.activeTimerPackage = null
    }

    fun isTimerActive(packageName: String): Boolean {
        return activeTimers.containsKey(packageName)
    }

    fun setCurrentForegroundPackage(packageName: String?) {
        currentForegroundPackage = packageName
    }

    fun getCurrentForegroundPackage(): String? {
        return currentForegroundPackage
    }

    fun updateLastActiveTime(context: Context, packageName: String) {
        val appRepository = AppRepository(context)
        appRepository.setLastActiveTimestamp(packageName, System.currentTimeMillis())
    }

    private fun startPeriodicPause(context: Context, packageName: String) {
        ServiceHelper.startPeriodicPause(context, packageName)
    }

    fun onAppLeft(context: Context, packageName: String) {
        // Quand l'utilisateur quitte l'app, on arrête le timer
        // Il sera relancé si l'utilisateur revient avant 5 minutes
        stopTimer(context, packageName)
    }

    /**
     * Restaure le timer si l'app a été tuée et redémarrée.
     * À appeler dans onServiceConnected() du service d'accessibilité.
     */
    fun restoreTimerIfNeeded(context: Context) {
        val preferencesManager = PreferencesManager(context)
        val activePackage = preferencesManager.activeTimerPackage ?: return

        // Vérifier si l'app est toujours configurée et a un timer
        val appRepository = AppRepository(context)
        val timerSeconds = appRepository.getAppPeriodicTimer(activePackage)
        if (timerSeconds <= 0) {
            preferencesManager.activeTimerPackage = null
            return
        }

        // Vérifier si on est dans la fenêtre de 5 minutes
        if (appRepository.shouldResetTimer(activePackage)) {
            // Plus de 5 minutes, on ne restaure pas
            preferencesManager.activeTimerPackage = null
            return
        }

        // Restaurer le timer - on suppose que l'utilisateur est toujours sur cette app
        currentForegroundPackage = activePackage
        startTimer(context, activePackage, timerSeconds)
    }

    fun onInitialPauseFinished(context: Context, packageName: String) {
        // Après la pause initiale, démarrer le timer périodique si configuré
        val appRepository = AppRepository(context)
        val timerSeconds = appRepository.getAppPeriodicTimer(packageName)
        if (timerSeconds > 0) {
            updateLastActiveTime(context, packageName)
            startTimer(context, packageName, timerSeconds)
        }
    }

    fun onPeriodicPauseFinished(context: Context, packageName: String) {
        // Après une pause périodique, le timer continue (déjà relancé dans le Runnable)
        updateLastActiveTime(context, packageName)
    }
}
