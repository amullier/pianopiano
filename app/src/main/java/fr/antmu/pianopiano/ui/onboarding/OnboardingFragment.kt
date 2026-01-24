package fr.antmu.pianopiano.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import fr.antmu.pianopiano.R
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.databinding.FragmentOnboardingBinding
import fr.antmu.pianopiano.util.PermissionHelper

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = PreferencesManager(requireContext())

        setupClickListeners()
        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupClickListeners() {
        binding.cardOverlay.setOnClickListener {
            PermissionHelper.requestOverlayPermission(requireContext())
            Toast.makeText(
                requireContext(),
                R.string.hint_enable_overlay,
                Toast.LENGTH_LONG
            ).show()
        }

        binding.cardAccessibility.setOnClickListener {
            PermissionHelper.openAccessibilitySettings(requireContext())
            Toast.makeText(
                requireContext(),
                R.string.hint_enable_accessibility,
                Toast.LENGTH_LONG
            ).show()
        }

        binding.cardUsageStats.setOnClickListener {
            PermissionHelper.openUsageAccessSettings(requireContext())
            Toast.makeText(
                requireContext(),
                R.string.hint_select_pianopiano,
                Toast.LENGTH_LONG
            ).show()
        }

        binding.buttonContinue.setOnClickListener {
            preferencesManager.onboardingCompleted = true
            findNavController().navigate(R.id.action_onboarding_to_main)
        }
    }

    private fun updatePermissionStatus() {
        val hasOverlay = PermissionHelper.hasOverlayPermission(requireContext())
        val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(requireContext())
        val hasUsageStats = PermissionHelper.hasUsageStatsPermission(requireContext())

        binding.iconOverlayStatus.visibility = if (hasOverlay) View.VISIBLE else View.GONE
        binding.iconAccessibilityStatus.visibility = if (hasAccessibility) View.VISIBLE else View.GONE
        binding.iconUsageStatsStatus.visibility = if (hasUsageStats) View.VISIBLE else View.GONE

        // Activer le bouton si toutes les permissions sont accordées
        val allGranted = hasOverlay && hasAccessibility && hasUsageStats
        binding.buttonContinue.isEnabled = allGranted
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
