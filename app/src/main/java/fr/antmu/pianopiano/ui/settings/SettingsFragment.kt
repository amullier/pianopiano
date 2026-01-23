package fr.antmu.pianopiano.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import fr.antmu.pianopiano.R
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

        binding.cardOverlay.setOnClickListener {
            viewModel.requestOverlayPermission()
        }

        binding.cardAccessibility.setOnClickListener {
            viewModel.openAccessibilitySettings()
        }

        binding.sliderDuration.setRange(6, 30, 6)
        binding.sliderDuration.setValue(viewModel.pauseDuration.value ?: 12)
        binding.sliderDuration.setOnValueChangeListener { value ->
            viewModel.setPauseDuration(value)
        }
    }

    private fun observeViewModel() {
        viewModel.hasOverlayPermission.observe(viewLifecycleOwner) { hasPermission ->
            updatePermissionStatus(
                binding.textOverlayStatus,
                hasPermission
            )
        }

        viewModel.hasAccessibilityPermission.observe(viewLifecycleOwner) { hasPermission ->
            updatePermissionStatus(
                binding.textAccessibilityStatus,
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
