package fr.antmu.pianopiano.ui.main

import android.graphics.Typeface
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

    private var showToday = true // true = aujourd'hui, false = total
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
        setupStatsToggle()
        observeViewModel()

        // Afficher le loader au début
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerApps.visibility = View.GONE
        binding.textNoResults.visibility = View.GONE

        viewModel.loadApps()
    }

    private fun setupSearch() {
        // Toggle search bar visibility with animation
        binding.buttonSearch.setOnClickListener {
            isSearchVisible = !isSearchVisible
            if (isSearchVisible) {
                showSearchBar()
            } else {
                hideSearchBar()
            }
        }

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Ne pas afficher le loader lors de la recherche
                binding.progressBar.visibility = View.GONE
                viewModel.searchApps(s?.toString() ?: "")
            }
        })
    }

    private fun showSearchBar() {
        binding.cardSearch.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationY = -height.toFloat().coerceAtLeast(40f)
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction {
                    binding.editSearch.requestFocus()
                }
                .start()
        }
    }

    private fun hideSearchBar() {
        binding.cardSearch.animate()
            .alpha(0f)
            .translationY(-binding.cardSearch.height.toFloat().coerceAtLeast(40f))
            .setDuration(250)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                binding.cardSearch.visibility = View.GONE
                binding.cardSearch.alpha = 1f
                binding.cardSearch.translationY = 0f
                binding.editSearch.text?.clear()
                viewModel.searchApps("")
            }
            .start()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshServiceStatus()
        viewModel.refreshPeanutCount()
        viewModel.refreshStats()
        // Ne pas réafficher le loader si on a déjà des données
    }

    private fun setupHeader() {
        val headerBinding = ViewHeaderBinding.bind(binding.header.root)

        // Styliser les deux "P" de PianoPiano en accent_primary
        val title = getString(R.string.app_name) // "PianoPiano"
        val spannable = SpannableString(title)
        val accentColor = ContextCompat.getColor(requireContext(), R.color.accent_peanut)

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
            },
            onInfoClicked = { app ->
                showAppStatsDialog(app)
            },
            hasUsageStatsPermission = fr.antmu.pianopiano.util.PermissionHelper.hasUsageStatsPermission(requireContext())
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

    private fun showAppStatsDialog(app: AppRepository.InstalledApp) {
        // Récupérer les stats
        val stats = UsageStatsHelper.getAppUsageStats(requireContext(), app.packageName)

        if (stats == null) {
            Toast.makeText(requireContext(), R.string.app_stats_no_data, Toast.LENGTH_SHORT).show()
            return
        }

        // Inflater et remplir la dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_app_stats, null)

        dialogView.findViewById<android.widget.TextView>(R.id.textAppName).text = app.appName

        // Aujourd'hui
        dialogView.findViewById<android.widget.TextView>(R.id.textScreenTimeToday).text =
            if (stats.totalTimeToday > 0) UsageStatsHelper.formatShortDuration(stats.totalTimeToday)
            else getString(R.string.stats_no_data)
        dialogView.findViewById<android.widget.TextView>(R.id.textLaunchCountToday).text =
            if (stats.launchCountToday > 0) stats.launchCountToday.toString()
            else getString(R.string.stats_no_data)

        // 7 derniers jours
        dialogView.findViewById<android.widget.TextView>(R.id.textScreenTime7Days).text =
            if (stats.totalTimeLast7Days > 0) UsageStatsHelper.formatShortDuration(stats.totalTimeLast7Days)
            else getString(R.string.stats_no_data)
        dialogView.findViewById<android.widget.TextView>(R.id.textLaunchCount7Days).text =
            if (stats.launchCountLast7Days > 0) stats.launchCountLast7Days.toString()
            else getString(R.string.stats_no_data)

        // 30 derniers jours
        dialogView.findViewById<android.widget.TextView>(R.id.textScreenTime30Days).text =
            if (stats.totalTimeLast30Days > 0) UsageStatsHelper.formatShortDuration(stats.totalTimeLast30Days)
            else getString(R.string.stats_no_data)
        dialogView.findViewById<android.widget.TextView>(R.id.textLaunchCount30Days).text =
            if (stats.launchCountLast30Days > 0) stats.launchCountLast30Days.toString()
            else getString(R.string.stats_no_data)

        // Afficher
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok, null)
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
            binding.progressBar.visibility = View.GONE
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
            updateStatsDisplay(stats)
        }

        viewModel.peanutsToday.observe(viewLifecycleOwner) {
            updatePreventedOpensDisplay()
        }

        viewModel.peanutsLast7Days.observe(viewLifecycleOwner) {
            updatePreventedOpensDisplay()
        }
    }

    private fun setupStatsToggle() {
        binding.toggleToday.setOnClickListener {
            if (!showToday) {
                showToday = true
                updateToggleUI()
                viewModel.aggregatedStats.value?.let { updateStatsDisplay(it) }
                updatePreventedOpensDisplay()
            }
        }

        binding.toggleTotal.setOnClickListener {
            if (showToday) {
                showToday = false
                updateToggleUI()
                viewModel.aggregatedStats.value?.let { updateStatsDisplay(it) }
                updatePreventedOpensDisplay()
            }
        }
    }

    private fun updateToggleUI() {
        if (showToday) {
            // Style "Aujourd'hui" sélectionné
            binding.toggleToday.setBackgroundResource(R.drawable.bg_button_gradient)
            binding.toggleToday.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.toggleToday.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

            // Style "Total" non sélectionné
            binding.toggleTotal.background = null
            binding.toggleTotal.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_tertiary))
            binding.toggleTotal.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        } else {
            // Style "Total" sélectionné
            binding.toggleTotal.setBackgroundResource(R.drawable.bg_button_gradient)
            binding.toggleTotal.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.toggleTotal.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

            // Style "Aujourd'hui" non sélectionné
            binding.toggleToday.background = null
            binding.toggleToday.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_tertiary))
            binding.toggleToday.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
    }

    private fun updateStatsDisplay(stats: UsageStatsHelper.AggregatedStats) {
        // Temps d'écran (selon le toggle)
        val screenTime = if (showToday) stats.totalScreenTimeToday else stats.totalScreenTime7Days

        if (screenTime > 0) {
            binding.textScreenTime.text = UsageStatsHelper.formatShortDuration(screenTime)
        } else {
            binding.textScreenTime.text = getString(R.string.stats_no_data)
        }
    }

    private fun updatePreventedOpensDisplay() {
        // Ouvertures empêchées (selon le toggle)
        val preventedOpens = if (showToday) {
            viewModel.peanutsToday.value ?: 0
        } else {
            viewModel.peanutsLast7Days.value ?: 0
        }

        binding.textPreventedOpens.text = preventedOpens.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
