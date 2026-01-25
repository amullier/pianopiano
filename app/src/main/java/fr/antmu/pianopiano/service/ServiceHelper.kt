package fr.antmu.pianopiano.service

import android.content.Context
import android.content.Intent
import fr.antmu.pianopiano.ui.pause.PauseActivity

object ServiceHelper {

    private val exemptedPackages = mutableMapOf<String, ExemptionInfo>()
    private const val EXEMPTION_DURATION_MS = 1000L // 1 seconde pour "Continuer"

    private data class ExemptionInfo(
        val timestamp: Long,
        val duration: Long
    )

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
     * Lance une pause périodique (alias de startPauseOverlay avec isPeriodic=true)
     */
    fun startPeriodicPause(context: Context, targetPackageName: String) {
        startPauseOverlay(context, targetPackageName, isPeriodic = true)
    }

    /**
     * Ajoute une exemption temporaire pour un package
     * (utilisé quand l'utilisateur clique sur Continuer)
     */
    fun exemptPackage(packageName: String) {
        exemptedPackages[packageName] = ExemptionInfo(
            timestamp = System.currentTimeMillis(),
            duration = EXEMPTION_DURATION_MS
        )
    }

    /**
     * Vérifie si un package est actuellement exempté
     */
    fun isPackageExempted(packageName: String): Boolean {
        val exemptionInfo = exemptedPackages[packageName] ?: return false
        val currentTime = System.currentTimeMillis()

        return if (currentTime - exemptionInfo.timestamp < exemptionInfo.duration) {
            true
        } else {
            // Nettoyer l'ancienne exemption
            exemptedPackages.remove(packageName)
            false
        }
    }
}
