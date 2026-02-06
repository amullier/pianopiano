package fr.antmu.pianopiano.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import fr.antmu.pianopiano.R
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupVersion()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }

    private fun setupUI() {
        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.cardAccessibility.setOnClickListener {
            viewModel.openAccessibilitySettings()
            showPermissionHint(R.string.hint_enable_accessibility)
        }

        binding.cardUsageStats.setOnClickListener {
            viewModel.openUsageAccessSettings()
            showPermissionHint(R.string.hint_select_pianopiano)
        }

        binding.sliderDuration.setValues(listOf(10, 15, 20, 30, 40, 50, 60))
        binding.sliderDuration.setValue(viewModel.pauseDuration.value ?: 10)
        binding.sliderDuration.setOnValueChangeListener { value ->
            viewModel.setPauseDuration(value)
        }

        binding.buttonReplayTutorial.setOnClickListener {
            PreferencesManager(requireContext()).tutorialCompleted = false
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewModel.hasAccessibilityPermission.observe(viewLifecycleOwner) { hasPermission ->
            updatePermissionStatus(
                binding.textAccessibilityStatus,
                hasPermission
            )
        }

        viewModel.hasUsageStatsPermission.observe(viewLifecycleOwner) { hasPermission ->
            updatePermissionStatus(
                binding.textUsageStatsStatus,
                hasPermission
            )
        }

        viewModel.pauseDuration.observe(viewLifecycleOwner) { duration ->
            binding.sliderDuration.setValue(duration)
        }
    }

    private fun updatePermissionStatus(textView: android.widget.TextView, granted: Boolean) {
        if (granted) {
            textView.text = getString(R.string.permission_granted)
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_active))
        } else {
            textView.text = getString(R.string.permission_not_granted)
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_inactive))
        }
    }

    private fun showPermissionHint(messageResId: Int) {
        Toast.makeText(
            requireContext(),
            messageResId,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun setupVersion() {
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(
                requireContext().packageName,
                0
            )
            binding.textVersion.text = "Version ${packageInfo.versionName}"
        } catch (e: Exception) {
            binding.textVersion.text = "Version inconnue"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
