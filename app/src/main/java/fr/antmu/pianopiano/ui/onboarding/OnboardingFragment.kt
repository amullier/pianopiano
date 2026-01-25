package fr.antmu.pianopiano.ui.onboarding

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
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

        setupTitle()
        setupClickListeners()
        updatePermissionStatus()
    }

    private fun setupTitle() {
        val title = getString(R.string.app_name) // "PianoPiano"
        val spannable = SpannableString(title)
        val accentColor = ContextCompat.getColor(requireContext(), R.color.accent_primary)

        // Premier "P" (index 0)
        spannable.setSpan(
            ForegroundColorSpan(accentColor),
            0, 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // Deuxième "P" (index 5 dans "PianoPiano")
        spannable.setSpan(
            ForegroundColorSpan(accentColor),
            5, 6,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.textTitle.text = spannable
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupClickListeners() {
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
            // Sauvegarder la version et marquer l'onboarding comme complété
            val currentVersionCode = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0).longVersionCode.toInt()
            preferencesManager.lastOnboardingVersion = currentVersionCode
            preferencesManager.onboardingCompleted = true
            findNavController().navigate(R.id.action_onboarding_to_main)
        }
    }

    private fun updatePermissionStatus() {
        val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(requireContext())
        val hasUsageStats = PermissionHelper.hasUsageStatsPermission(requireContext())

        binding.iconAccessibilityStatus.visibility = if (hasAccessibility) View.VISIBLE else View.GONE
        binding.iconUsageStatsStatus.visibility = if (hasUsageStats) View.VISIBLE else View.GONE

        // Activer le bouton si toutes les permissions sont accordées
        val allGranted = hasAccessibility && hasUsageStats
        binding.buttonContinue.isEnabled = allGranted
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
