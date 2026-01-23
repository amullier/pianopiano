package fr.antmu.pianopiano.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.antmu.pianopiano.data.repository.SettingsRepository
import fr.antmu.pianopiano.util.PermissionHelper

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    private val _hasOverlayPermission = MutableLiveData<Boolean>()
    val hasOverlayPermission: LiveData<Boolean> = _hasOverlayPermission

    private val _hasAccessibilityPermission = MutableLiveData<Boolean>()
    val hasAccessibilityPermission: LiveData<Boolean> = _hasAccessibilityPermission

    private val _pauseDuration = MutableLiveData<Int>()
    val pauseDuration: LiveData<Int> = _pauseDuration

    init {
        _pauseDuration.value = settingsRepository.pauseDuration
    }

    fun refreshPermissions() {
        val context = getApplication<Application>()
        _hasOverlayPermission.value = PermissionHelper.hasOverlayPermission(context)
        _hasAccessibilityPermission.value = PermissionHelper.isAccessibilityServiceEnabled(context)
    }

    fun setPauseDuration(duration: Int) {
        settingsRepository.pauseDuration = duration
        _pauseDuration.value = duration
    }

    fun requestOverlayPermission() {
        PermissionHelper.requestOverlayPermission(getApplication())
    }

    fun openAccessibilitySettings() {
        PermissionHelper.openAccessibilitySettings(getApplication())
    }
}
