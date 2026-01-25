package fr.antmu.pianopiano.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fr.antmu.pianopiano.data.repository.AppRepository
import fr.antmu.pianopiano.data.repository.SettingsRepository
import fr.antmu.pianopiano.ui.components.ServiceStatusView
import fr.antmu.pianopiano.util.PermissionHelper
import fr.antmu.pianopiano.util.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private var allApps: List<AppRepository.InstalledApp> = emptyList()
    private var currentSearchQuery: String = ""

    private val _apps = MutableLiveData<List<AppRepository.InstalledApp>>()
    val apps: LiveData<List<AppRepository.InstalledApp>> = _apps

    private val _serviceStatus = MutableLiveData<ServiceStatusView.ServiceStatus>()
    val serviceStatus: LiveData<ServiceStatusView.ServiceStatus> = _serviceStatus

    private val _peanutCount = MutableLiveData<Int>()
    val peanutCount: LiveData<Int> = _peanutCount

    private val _peanutsToday = MutableLiveData<Int>()
    val peanutsToday: LiveData<Int> = _peanutsToday

    private val _peanutsLast7Days = MutableLiveData<Int>()
    val peanutsLast7Days: LiveData<Int> = _peanutsLast7Days

    private val _aggregatedStats = MutableLiveData<UsageStatsHelper.AggregatedStats>()
    val aggregatedStats: LiveData<UsageStatsHelper.AggregatedStats> = _aggregatedStats

    fun loadApps() {
        viewModelScope.launch {
            val installedApps = withContext(Dispatchers.IO) {
                appRepository.getInstalledUserApps()
            }
            allApps = installedApps
            filterApps(currentSearchQuery)
        }
    }

    fun searchApps(query: String) {
        currentSearchQuery = query
        filterApps(query)
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter { app ->
                app.appName.contains(query, ignoreCase = true)
            }
        }
        _apps.value = filtered
    }

    fun refreshServiceStatus() {
        val context = getApplication<Application>()
        val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(context)
        val hasOverlay = PermissionHelper.hasOverlayPermission(context)
        val hasUsageStats = PermissionHelper.hasUsageStatsPermission(context)

        _serviceStatus.value = when {
            !hasAccessibility || !hasOverlay -> ServiceStatusView.ServiceStatus.INACTIVE
            !hasUsageStats -> ServiceStatusView.ServiceStatus.PARTIAL
            else -> ServiceStatusView.ServiceStatus.ACTIVE
        }
    }

    fun refreshPeanutCount() {
        _peanutCount.value = settingsRepository.peanutCount
    }

    fun refreshStats() {
        viewModelScope.launch {
            val stats = withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                val configuredPackages = appRepository.getConfiguredAppPackages()
                val peanuts = settingsRepository.peanutCount
                val peanutsToday = settingsRepository.peanutsToday
                val peanutsLast7Days = settingsRepository.peanutsLast7Days

                // Mettre à jour les peanuts
                _peanutsToday.postValue(peanutsToday)
                _peanutsLast7Days.postValue(peanutsLast7Days)

                UsageStatsHelper.getAggregatedStats(context, configuredPackages, peanuts, peanutsToday, peanutsLast7Days)
            }
            _aggregatedStats.value = stats
        }
    }

    fun setAppConfigured(app: AppRepository.InstalledApp, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            // Sauvegarder la configuration
            appRepository.setAppConfigured(app.packageName, app.appName, enabled)

            // Mettre à jour le cache en mémoire
            allApps = allApps.map { installedApp ->
                if (installedApp.packageName == app.packageName) {
                    installedApp.copy(isConfigured = enabled)
                } else {
                    installedApp
                }
            }

            // Rafraîchir la liste filtrée sur le thread principal
            withContext(Dispatchers.Main) {
                filterApps(currentSearchQuery)
            }
        }
    }

    fun openAccessibilitySettings() {
        PermissionHelper.openAccessibilitySettings(getApplication())
    }

    fun getAppPeriodicTimer(packageName: String): Int {
        return appRepository.getAppPeriodicTimer(packageName)
    }

    fun setAppPeriodicTimer(packageName: String, seconds: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.setAppPeriodicTimer(packageName, seconds)
        }
    }
}
