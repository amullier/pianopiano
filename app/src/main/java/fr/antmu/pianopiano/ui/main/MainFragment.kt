package fr.antmu.pianopiano.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import fr.antmu.pianopiano.R
import fr.antmu.pianopiano.data.repository.AppRepository
import fr.antmu.pianopiano.databinding.FragmentMainBinding
import fr.antmu.pianopiano.databinding.ViewHeaderBinding
import fr.antmu.pianopiano.util.setVisible

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: AppListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupRecyclerView()
        setupActivationButton()
        observeViewModel()

        viewModel.loadApps()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshServiceStatus()
        viewModel.refreshPeanutCount()
    }

    private fun setupHeader() {
        val headerBinding = ViewHeaderBinding.bind(binding.header.root)

        headerBinding.buttonSettings.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_settings)
        }
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter(
            onToggleChanged = { app, enabled ->
                viewModel.setAppConfigured(app, enabled)
            },
            onSettingsClicked = { app ->
                showTimerConfigDialog(app)
            }
        )

        binding.recyclerApps.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerApps.adapter = adapter
    }

    private fun showTimerConfigDialog(app: AppRepository.InstalledApp) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_timer_config, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupTimer)

        // Récupérer la valeur actuelle du timer
        val currentTimer = viewModel.getAppPeriodicTimer(app.packageName)

        // Sélectionner le bon radio button
        val radioId = when (currentTimer) {
            0 -> R.id.radioDisabled
            30 -> R.id.radio30sec
            300 -> R.id.radio5min
            900 -> R.id.radio15min
            1800 -> R.id.radio30min
            3600 -> R.id.radio1h
            else -> R.id.radioDisabled
        }
        radioGroup.check(radioId)

        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val selectedTimer = when (radioGroup.checkedRadioButtonId) {
                    R.id.radioDisabled -> 0
                    R.id.radio30sec -> 30
                    R.id.radio5min -> 300
                    R.id.radio15min -> 900
                    R.id.radio30min -> 1800
                    R.id.radio1h -> 3600
                    else -> 0
                }
                viewModel.setAppPeriodicTimer(app.packageName, selectedTimer)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupActivationButton() {
        binding.buttonActivateService.setOnClickListener {
            viewModel.openAccessibilitySettings()
            Toast.makeText(
                requireContext(),
                R.string.hint_enable_accessibility,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun observeViewModel() {
        viewModel.apps.observe(viewLifecycleOwner) { apps ->
            adapter.submitList(apps)
//            binding.textNoApps.setVisible(apps.isEmpty())
        }

        viewModel.serviceStatus.observe(viewLifecycleOwner) { status ->
            binding.serviceStatus.setStatus(status)

            when (status) {
                fr.antmu.pianopiano.ui.components.ServiceStatusView.ServiceStatus.INACTIVE -> {
                    binding.mainServiceEnabler.visibility = View.VISIBLE
                    binding.serviceStatusCard.visibility = View.GONE
                }
                fr.antmu.pianopiano.ui.components.ServiceStatusView.ServiceStatus.PARTIAL,
                fr.antmu.pianopiano.ui.components.ServiceStatusView.ServiceStatus.ACTIVE -> {
                    binding.mainServiceEnabler.visibility = View.GONE
                    binding.serviceStatusCard.visibility = View.VISIBLE
                    binding.serviceStatusCardView.setStatus(status)
                }
            }
        }

        viewModel.peanutCount.observe(viewLifecycleOwner) { count ->
            val headerBinding = ViewHeaderBinding.bind(binding.header.root)
            headerBinding.textPeanuts.text = getString(R.string.peanut_count_format, count)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
