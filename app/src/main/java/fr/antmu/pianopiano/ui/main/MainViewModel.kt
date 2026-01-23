package fr.antmu.pianopiano.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fr.antmu.pianopiano.data.repository.AppRepository
import fr.antmu.pianopiano.data.repository.SettingsRepository
import fr.antmu.pianopiano.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _apps = MutableLiveData<List<AppRepository.InstalledApp>>()
    val apps: LiveData<List<AppRepository.InstalledApp>> = _apps

    private val _isServiceActive = MutableLiveData<Boolean>()
    val isServiceActive: LiveData<Boolean> = _isServiceActive

    private val _peanutCount = MutableLiveData<Int>()
    val peanutCount: LiveData<Int> = _peanutCount

    fun loadApps() {
        viewModelScope.launch {
            val installedApps = withContext(Dispatchers.IO) {
                appRepository.getInstalledUserApps()
            }
            _apps.value = installedApps
        }
    }

    fun refreshServiceStatus() {
        val context = getApplication<Application>()
        _isServiceActive.value = PermissionHelper.isAccessibilityServiceEnabled(context) &&
                PermissionHelper.hasOverlayPermission(context)
    }

    fun refreshPeanutCount() {
        _peanutCount.value = settingsRepository.peanutCount
    }

    fun setAppConfigured(app: AppRepository.InstalledApp, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.setAppConfigured(app.packageName, app.appName, enabled)
        }
    }

    fun openAccessibilitySettings() {
        PermissionHelper.openAccessibilitySettings(getApplication())
    }
}
