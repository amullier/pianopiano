package fr.antmu.pianopiano.ui.pause

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.antmu.pianopiano.data.repository.SettingsRepository
import fr.antmu.pianopiano.util.PackageHelper
import fr.antmu.pianopiano.util.PermissionHelper
import fr.antmu.pianopiano.util.QuotesProvider
import fr.antmu.pianopiano.util.UsageStatsHelper

class PauseViewModel(context: Context) {

    private val settingsRepository = SettingsRepository(context)
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

    fun initialize(packageName: String) {
        targetPackageName = packageName
        _appName.value = PackageHelper.getAppName(appContext, packageName)
        _quote.value = QuotesProvider.getRandomQuote(appContext)
        _isCountdownFinished.value = false

        // Check permission and load usage stats
        _hasUsageStatsPermission.value = PermissionHelper.hasUsageStatsPermission(appContext)
        if (_hasUsageStatsPermission.value == true) {
            _usageStats.value = UsageStatsHelper.getAppUsageStats(appContext, packageName)
        }

        startCountdown()
    }

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
    }

    fun onContinueClicked(): Boolean {
        return PackageHelper.launchApp(appContext, targetPackageName)
    }

    fun getTargetPackageName(): String = targetPackageName

    fun cleanup() {
        countDownTimer?.cancel()
        countDownTimer = null
    }
}
