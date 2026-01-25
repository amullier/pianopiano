package fr.antmu.pianopiano.service

import android.content.Context
import android.content.Intent
import fr.antmu.pianopiano.ui.pause.PauseActivity

object ServiceHelper {

    /**
     * Lance la PauseActivity pour afficher l'écran de pause modal
     */
    fun startPauseOverlay(context: Context, targetPackageName: String, isPeriodic: Boolean = false) {
        val intent = Intent(context, PauseActivity::class.java).apply {
            putExtra(PauseActivity.EXTRA_PACKAGE_NAME, targetPackageName)
            putExtra(PauseActivity.EXTRA_IS_PERIODIC, isPeriodic)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }

    /**
     * Lance une pause périodique
     */
    fun startPeriodicPause(context: Context, targetPackageName: String) {
        startPauseOverlay(context, targetPackageName, isPeriodic = true)
    }
}
