package fr.antmu.pianopiano.ui.pause

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.service.AppLaunchDetectorService

class PauseActivity : AppCompatActivity() {

    private lateinit var pauseView: PauseOverlayView
    private var userMadeChoice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Récupérer les extras
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val isPeriodic = intent.getBooleanExtra(EXTRA_IS_PERIODIC, false)

        // Créer et afficher la vue de pause
        pauseView = PauseOverlayView(this, this) {
            // onDismiss callback — l'utilisateur a fait un choix (Cancel ou Continue)
            userMadeChoice = true
            AppLaunchDetectorService.activePauseForPackage = null
            finish()
        }

        setContentView(pauseView)

        // Initialiser la vue avec le package et le type de pause
        pauseView.show(packageName, isPeriodic)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        userMadeChoice = false

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val isPeriodic = intent.getBooleanExtra(EXTRA_IS_PERIODIC, false)

        pauseView.show(packageName, isPeriodic)
    }

    override fun onStop() {
        super.onStop()
        if (!userMadeChoice) {
            // L'utilisateur a quitté sans faire de choix (swipe, autre app...)
            // Poser forceNextPause pour que la prochaine ouverture relance la pause
            val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
            if (pkg.isNotEmpty()) {
                PreferencesManager(this).setForceNextPause(pkg, true)
            }
            AppLaunchDetectorService.activePauseForPackage = null
            finish()
        }
    }

    override fun onBackPressed() {
        // BLOQUER le bouton retour - ne rien faire
        // L'utilisateur DOIT cliquer Cancel ou Continue
    }

    override fun onDestroy() {
        super.onDestroy()
        pauseView.cleanup()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_IS_PERIODIC = "is_periodic"
    }
}
