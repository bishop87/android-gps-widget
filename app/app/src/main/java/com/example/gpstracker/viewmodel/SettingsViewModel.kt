package com.example.gpstracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.gpstracker.data.preferences.AppSettings
import com.example.gpstracker.data.repository.SettingsRepository

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepository = SettingsRepository(app)

    private val _settings = MutableLiveData<AppSettings>()
    val settings: LiveData<AppSettings> get() = _settings

    private val _saved = MutableLiveData<Boolean>()
    val saved: LiveData<Boolean> get() = _saved

    private val _validationError = MutableLiveData<String?>()
    val validationError: LiveData<String?> get() = _validationError

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _settings.value = settingsRepository.loadSettings()
    }

    fun saveSettings(
        deviceName: String,
        apiUrl: String,
        username: String,
        password: String,
        trackingIntervalSec: Int,
        widgetBackgroundColor: Int,
        overlayBackgroundColor: Int
    ) {
        if (apiUrl.isNotBlank() && !apiUrl.startsWith("https://")) {
            _validationError.value = "L'URL API deve iniziare con https://"
            return
        }
        if (trackingIntervalSec < 10) {
            _validationError.value = "Intervallo minimo: 10 secondi"
            return
        }

        _validationError.value = null
        val currentTracking = settingsRepository.isTrackingEnabled()
        val currentOverlay = settingsRepository.isOverlayEnabled()
        val newSettings = AppSettings(
            deviceName = deviceName,
            apiUrl = apiUrl,
            username = username,
            password = password,
            trackingEnabled = currentTracking,
            trackingIntervalSec = trackingIntervalSec,
            widgetBackgroundColor = widgetBackgroundColor,
            overlayEnabled = currentOverlay,
            overlayBackgroundColor = overlayBackgroundColor
        )
        settingsRepository.saveSettings(newSettings)
        _settings.value = newSettings
        _saved.value = true
    }
}
