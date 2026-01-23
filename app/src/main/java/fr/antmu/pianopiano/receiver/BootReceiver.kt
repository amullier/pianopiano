package fr.antmu.pianopiano.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.antmu.pianopiano.util.PermissionHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // The AccessibilityService will auto-restart if enabled
            // We just need to ensure permissions are still valid
            if (PermissionHelper.areAllPermissionsGranted(context)) {
                // Service will be restarted by the system
            }
        }
    }
}
