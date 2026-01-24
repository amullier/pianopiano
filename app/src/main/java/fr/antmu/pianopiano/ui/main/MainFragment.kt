package fr.antmu.pianopiano.ui.main

import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import fr.antmu.pianopiano.R
import fr.antmu.pianopiano.data.repository.AppRepository
import fr.antmu.pianopiano.databinding.FragmentMainBinding
import fr.antmu.pianopiano.databinding.ViewHeaderBinding
import fr.antmu.pianopiano.util.UsageStatsHelper
import fr.antmu.pianopiano.util.setVisible

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: AppListAdapter

    private var showTotalTimeSaved = true // true = total, false = aujourd'hui
    private var isSearchVisible = false

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
        setupSearch()
        setupRecyclerView()
        setupActivationButton()
        observeViewModel()

        viewModel.loadApps()
    }

    private fun setupSearch() {
        // Toggle search bar visibility
        binding.buttonSearch.setOnClickListener {
            isSearchVisible = !isSearchVisible
            binding.cardSearch.visibility = if (isSearchVisible) View.VISIBLE else View.GONE
            if (isSearchVisible) {
                binding.editSearch.requestFocus()
            } else {
                binding.editSearch.text?.clear()
                viewModel.searchApps("")
            }
        }

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchApps(s?.toString() ?: "")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshServiceStatus()
        viewModel.refreshPeanutCount()
        viewModel.refreshStats()
    }

    private fun setupHeader() {
        val headerBinding = ViewHeaderBinding.bind(binding.header.root)

        // Styliser les deux "P" de PianoPiano en accent_primary
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
        headerBinding.textTitle.text = spannable

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
            val hasResults = apps.isNotEmpty()
            binding.recyclerApps.visibility = if (hasResults) View.VISIBLE else View.GONE
            binding.textNoResults.visibility = if (hasResults) View.GONE else View.VISIBLE
        }

        viewModel.serviceStatus.observe(viewLifecycleOwner) { status ->
            binding.serviceStatus.setStatus(status)

            when (status) {
                fr.antmu.pianopiano.ui.components.ServiceStatusView.ServiceStatus.INACTIVE -> {
                    binding.mainServiceEnabler.visibility = View.VISIBLE
                    binding.serviceStatusCard.visibility = View.GONE
                }
                fr.antmu.pianopiano.ui.components.ServiceStatusView.ServiceStatus.PARTIAL -> {
                    binding.mainServiceEnabler.visibility = View.GONE
                    binding.serviceStatusCard.visibility = View.VISIBLE
                    binding.serviceStatusCardView.setStatus(status)
                }
                fr.antmu.pianopiano.ui.components.ServiceStatusView.ServiceStatus.ACTIVE -> {
                    // Masquer les deux cartes quand le service est actif
                    binding.mainServiceEnabler.visibility = View.GONE
                    binding.serviceStatusCard.visibility = View.GONE
                }
            }
        }

        viewModel.peanutCount.observe(viewLifecycleOwner) { count ->
            val headerBinding = ViewHeaderBinding.bind(binding.header.root)
            headerBinding.textPeanuts.text = getString(R.string.peanut_count_format, count)
        }

        viewModel.aggregatedStats.observe(viewLifecycleOwner) { stats ->
            if (stats.totalScreenTimeToday > 0) {
                binding.textScreenTime.text = UsageStatsHelper.formatShortDuration(stats.totalScreenTimeToday)
            } else {
                binding.textScreenTime.text = getString(R.string.stats_no_data)
            }

            updateTimeSavedDisplay(stats)
        }

        // Click listener pour basculer entre total et aujourd'hui
        binding.cardTimeSaved.setOnClickListener {
            showTotalTimeSaved = !showTotalTimeSaved
            viewModel.aggregatedStats.value?.let { updateTimeSavedDisplay(it) }
        }
    }

    private fun updateTimeSavedDisplay(stats: UsageStatsHelper.AggregatedStats) {
        val timeSaved = if (showTotalTimeSaved) stats.timeSavedTotal else stats.timeSavedToday
        val labelRes = if (showTotalTimeSaved) R.string.stats_time_saved_total else R.string.stats_time_saved_today

        binding.textTimeSavedLabel.text = getString(labelRes)

        if (timeSaved > 0) {
            binding.textTimeSaved.text = UsageStatsHelper.formatShortDuration(timeSaved)
        } else {
            binding.textTimeSaved.text = getString(R.string.stats_no_data)
        }

        // Mettre à jour les indicateurs
        binding.dotTotal.setBackgroundResource(
            if (showTotalTimeSaved) R.drawable.indicator_active else R.drawable.indicator_inactive
        )
        binding.dotToday.setBackgroundResource(
            if (showTotalTimeSaved) R.drawable.indicator_inactive else R.drawable.indicator_active
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
