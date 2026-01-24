package fr.antmu.pianopiano.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import fr.antmu.pianopiano.util.PermissionHelper

/**
 * Service lancé au boot pour initialiser l'application
 * et s'assurer que le service d'accessibilité fonctionne correctement.
 */
class BootInitService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BootInitService", "Service started - Initializing app")

        // Vérifier que les permissions sont toujours actives
        val hasPermissions = PermissionHelper.areAllPermissionsGranted(this)
        Log.d("BootInitService", "Permissions granted: $hasPermissions")

        // L'Application.onCreate() sera appelé automatiquement,
        // ce qui initialise PreferencesManager, AppRepository, etc.

        // Arrêter le service immédiatement après l'initialisation
        stopSelf()

        return START_NOT_STICKY
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, BootInitService::class.java)
            context.startService(intent)
        }
    }
}
