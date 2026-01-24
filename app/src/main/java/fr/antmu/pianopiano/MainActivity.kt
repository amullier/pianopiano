package fr.antmu.pianopiano

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.databinding.ActivityMainBinding
import fr.antmu.pianopiano.util.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Vérifier si l'onboarding doit être affiché
        val preferencesManager = PreferencesManager(this)
        val allPermissionsGranted = PermissionHelper.hasOverlayPermission(this) &&
                PermissionHelper.isAccessibilityServiceEnabled(this) &&
                PermissionHelper.hasUsageStatsPermission(this)

        if (preferencesManager.onboardingCompleted || allPermissionsGranted) {
            // Marquer l'onboarding comme complété si toutes les permissions sont accordées
            if (allPermissionsGranted) {
                preferencesManager.onboardingCompleted = true
            }
            // Naviguer directement vers l'écran principal
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
            navGraph.setStartDestination(R.id.mainFragment)
            navController.graph = navGraph
        }
    }
}
