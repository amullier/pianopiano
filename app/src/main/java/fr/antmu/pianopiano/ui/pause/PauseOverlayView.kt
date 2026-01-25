package fr.antmu.pianopiano.ui.pause

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import fr.antmu.pianopiano.R
import fr.antmu.pianopiano.databinding.ViewPauseOverlayBinding
import fr.antmu.pianopiano.service.ServiceHelper
import fr.antmu.pianopiano.util.setVisible

class PauseOverlayView(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    private val binding: ViewPauseOverlayBinding
    private val viewModel: PauseViewModel

    init {
        binding = ViewPauseOverlayBinding.inflate(LayoutInflater.from(context), this, true)
        viewModel = PauseViewModel(context)

        setupListeners()
        observeViewModel()
    }

    fun show(packageName: String, isPeriodic: Boolean = false) {
        viewModel.initialize(packageName, isPeriodic)
        binding.buttonContinue.visibility = View.INVISIBLE
    }

    private fun setupListeners() {
        binding.buttonCancel.setOnClickListener {
            viewModel.onCancelClicked()

            // PAS d'exemption pour Annuler
            // Si l'utilisateur rouvre l'app, il aura de nouveau une pause

            // Retour à l'écran d'accueil
            val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_HOME)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
            onDismiss()
        }

        binding.buttonContinue.setOnClickListener {
            // Exempter l'app pour permettre l'ouverture sans pause (1 seconde)
            ServiceHelper.exemptPackage(viewModel.getTargetPackageName())

            viewModel.onContinueClicked()
            onDismiss()
        }
    }

    private fun observeViewModel() {
        viewModel.isCountdownFinished.observe(lifecycleOwner) { finished ->
            binding.buttonContinue.setVisible(finished)
            if (finished) {
                val appName = viewModel.appName.value ?: ""
                binding.buttonContinue.text = context.getString(R.string.pause_continue, appName)
            }
        }

        viewModel.quote.observe(lifecycleOwner) { quote ->
            binding.textQuote.text = quote
        }

        viewModel.hasUsageStatsPermission.observe(lifecycleOwner) { hasPermission ->
            binding.statsCard.setVisible(hasPermission)
        }

        viewModel.usageStats.observe(lifecycleOwner) { stats ->
            stats?.let {
                binding.textTotalTime.text = fr.antmu.pianopiano.util.UsageStatsHelper.formatShortDuration(it.totalTimeToday)
                binding.textLaunchCount.text = it.launchCountToday.toString()
                binding.textAverageSession.text = fr.antmu.pianopiano.util.UsageStatsHelper.formatShortDuration(it.averageSessionTime)
            }
        }
    }

    fun cleanup() {
        viewModel.cleanup()
    }
}
