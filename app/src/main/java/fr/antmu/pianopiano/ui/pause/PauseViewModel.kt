package fr.antmu.pianopiano.ui.pause

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.antmu.pianopiano.data.local.PreferencesManager
import fr.antmu.pianopiano.data.repository.SettingsRepository
import fr.antmu.pianopiano.service.PeriodicTimerManager
import fr.antmu.pianopiano.util.PackageHelper
import fr.antmu.pianopiano.util.PermissionHelper
import fr.antmu.pianopiano.util.QuotesProvider
import fr.antmu.pianopiano.util.UsageStatsHelper

class PauseViewModel(context: Context) {

    private val settingsRepository = SettingsRepository(context)
    private val preferencesManager = PreferencesManager(context)
    private val appContext = context.applicationContext

    private val _countdown = MutableLiveData<Int>()
    val countdown: LiveData<Int> = _countdown

    private val _isCountdownFinished = MutableLiveData<Boolean>()
    val isCountdownFinished: LiveData<Boolean> = _isCountdownFinished

    private val _quote = MutableLiveData<String>()
    val quote: LiveData<String> = _quote

    private val _appName = MutableLiveData<String>()
    val appName: LiveData<String> = _appName

    private val _usageStats = MutableLiveData<UsageStatsHelper.AppUsageStats?>()
    val usageStats: LiveData<UsageStatsHelper.AppUsageStats?> = _usageStats

    private val _hasUsageStatsPermission = MutableLiveData<Boolean>()
    val hasUsageStatsPermission: LiveData<Boolean> = _hasUsageStatsPermission

    private var countDownTimer: CountDownTimer? = null
    private var targetPackageName: String = ""
    private var isPeriodic: Boolean = false

    fun initialize(packageName: String, periodic: Boolean = false) {
        targetPackageName = packageName
        isPeriodic = periodic
        _appName.value = PackageHelper.getAppName(appContext, packageName)
        _quote.value = QuotesProvider.getRandomQuote(appContext)
        _isCountdownFinished.value = false

        // Check permission and load usage stats
        _hasUsageStatsPermission.value = PermissionHelper.hasUsageStatsPermission(appContext)
        if (_hasUsageStatsPermission.value == true) {
            val rawStats = UsageStatsHelper.getAppUsageStats(appContext, packageName)
            if (rawStats != null) {
                val blockedCount = preferencesManager.getPeanutsForAppToday(packageName)
                val realLaunchCount = maxOf(0, rawStats.launchCountToday - blockedCount)
                val realAverageSession = if (realLaunchCount > 0) rawStats.totalTimeToday / realLaunchCount else 0L
                _usageStats.value = rawStats.copy(
                    launchCountToday = realLaunchCount,
                    averageSessionTime = realAverageSession
                )
            } else {
                _usageStats.value = null
            }
        }

        startCountdown()
    }

    fun isPeriodic(): Boolean = isPeriodic

    private fun startCountdown() {
        val duration = settingsRepository.pauseDuration
        _countdown.value = duration

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer((duration * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _countdown.value = (millisUntilFinished / 1000).toInt() + 1
            }

            override fun onFinish() {
                _countdown.value = 0
                _isCountdownFinished.value = true
            }
        }.start()
    }

    fun onCancelClicked() {
        countDownTimer?.cancel()
        settingsRepository.incrementPeanuts()
        preferencesManager.incrementPeanutsForApp(targetPackageName)
        // Set flag to force pause on next access (bypasses debounce)
        preferencesManager.setForceNextPause(targetPackageName, true)
    }

    fun onContinueClicked(): Boolean {
        // Mettre à jour exitTime pour éviter une nouvelle pause immédiate
        // (la tolérance de 5s recommence maintenant)
        preferencesManager.setAppExitTime(targetPackageName, System.currentTimeMillis())

        // Gérer le timer périodique
        if (isPeriodic) {
            PeriodicTimerManager.onPeriodicPauseFinished(appContext, targetPackageName)
        } else {
            PeriodicTimerManager.onInitialPauseFinished(appContext, targetPackageName)
        }
        return PackageHelper.launchApp(appContext, targetPackageName)
    }

    fun getTargetPackageName(): String = targetPackageName

    fun cleanup() {
        countDownTimer?.cancel()
        countDownTimer = null
    }
}
