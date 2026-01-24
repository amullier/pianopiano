package fr.antmu.pianopiano.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import fr.antmu.pianopiano.service.BootInitService
import fr.antmu.pianopiano.util.PermissionHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed - Starting initialization")

            // Lancer le service d'initialisation pour garantir que l'application
            // est prête à fonctionner, même si elle n'a pas été lancée manuellement
            try {
                BootInitService.start(context)
                Log.d("BootReceiver", "BootInitService started successfully")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to start BootInitService", e)
            }
        }
    }
}
