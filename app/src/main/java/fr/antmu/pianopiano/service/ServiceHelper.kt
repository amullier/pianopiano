package fr.antmu.pianopiano.service

import android.content.Context
import android.content.Intent
import android.os.Build
import fr.antmu.pianopiano.util.PermissionHelper

object ServiceHelper {

    private val exemptedPackages = mutableMapOf<String, Long>()
    private const val EXEMPTION_DURATION_MS = 10000L // 10 secondes

    fun startPauseOverlay(context: Context, targetPackageName: String, isPeriodic: Boolean = false) {
        if (!PermissionHelper.hasOverlayPermission(context)) {
            return
        }

        val intent = Intent(context, PauseOverlayService::class.java).apply {
            action = PauseOverlayService.ACTION_SHOW
            putExtra(PauseOverlayService.EXTRA_PACKAGE_NAME, targetPackageName)
            putExtra(PauseOverlayService.EXTRA_IS_PERIODIC, isPeriodic)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun startPeriodicPause(context: Context, targetPackageName: String) {
        startPauseOverlay(context, targetPackageName, isPeriodic = true)
    }

    fun stopPauseOverlay(context: Context) {
        val intent = Intent(context, PauseOverlayService::class.java).apply {
            action = PauseOverlayService.ACTION_HIDE
        }
        context.startService(intent)
    }

    fun isOverlayShowing(): Boolean {
        return PauseOverlayService.isShowing
    }

    /**
     * Ajoute une exemption temporaire pour un package
     * (utilisé quand l'utilisateur clique sur Annuler ou Continuer)
     */
    fun exemptPackage(packageName: String) {
        exemptedPackages[packageName] = System.currentTimeMillis()
    }

    /**
     * Vérifie si un package est actuellement exempté
     */
    fun isPackageExempted(packageName: String): Boolean {
        val exemptionTime = exemptedPackages[packageName] ?: return false
        val currentTime = System.currentTimeMillis()

        return if (currentTime - exemptionTime < EXEMPTION_DURATION_MS) {
            true
        } else {
            // Nettoyer l'ancienne exemption
            exemptedPackages.remove(packageName)
            false
        }
    }
}
