package com.example.gpstracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gpstracker.data.repository.LocationRepository
import com.example.gpstracker.data.repository.SettingsRepository
import com.example.gpstracker.domain.usecase.GetCurrentLocationUseCase
import com.example.gpstracker.domain.usecase.SendLocationUseCase
import com.example.gpstracker.domain.usecase.StartTrackingUseCase
import com.example.gpstracker.domain.usecase.StopTrackingUseCase
import kotlinx.coroutines.launch

enum class UiState { IDLE, ACQUIRING, SENDING, SUCCESS, ERROR }

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepository = SettingsRepository(app)
    private val locationRepository = LocationRepository(app)
    private val getCurrentLocation = GetCurrentLocationUseCase(locationRepository)
    private val sendLocation = SendLocationUseCase(settingsRepository)
    private val startTracking = StartTrackingUseCase(app)
    private val stopTracking = StopTrackingUseCase(app)

    private val _uiState = MutableLiveData<UiState>(UiState.IDLE)
    val uiState: LiveData<UiState> get() = _uiState

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> get() = _statusMessage

    private val _trackingEnabled = MutableLiveData<Boolean>()
    val trackingEnabled: LiveData<Boolean> get() = _trackingEnabled

    private val _overlayEnabled = MutableLiveData<Boolean>()
    val overlayEnabled: LiveData<Boolean> get() = _overlayEnabled

    init {
        _trackingEnabled.value = settingsRepository.isTrackingEnabled()
        _overlayEnabled.value = settingsRepository.isOverlayEnabled()
    }

    fun testApiCall() {
        if (_uiState.value == UiState.ACQUIRING || _uiState.value == UiState.SENDING) return

        viewModelScope.launch {
            _uiState.value = UiState.ACQUIRING
            val locationResult = getCurrentLocation.execute()

            locationResult.onSuccess { location ->
                _uiState.value = UiState.SENDING
                sendLocation.execute(location).onSuccess {
                    _uiState.value = UiState.SUCCESS
                    _statusMessage.value = "Posizione inviata con successo"
                }.onFailure { e ->
                    _uiState.value = UiState.ERROR
                    _statusMessage.value = e.message ?: "Errore invio"
                }
            }.onFailure { e ->
                _uiState.value = UiState.ERROR
                _statusMessage.value = e.message ?: "GPS non disponibile"
            }
        }
    }

    fun toggleTracking(enabled: Boolean) {
        settingsRepository.setTrackingEnabled(enabled)
        _trackingEnabled.value = enabled
        if (enabled) {
            startTracking.execute()
        } else {
            stopTracking.execute()
        }
    }

    fun toggleOverlay(enabled: Boolean) {
        settingsRepository.setOverlayEnabled(enabled)
        _overlayEnabled.value = enabled
        val intent = android.content.Intent(getApplication(), com.example.gpstracker.service.OverlayService::class.java)
        if (enabled) {
            getApplication<Application>().startService(intent)
        } else {
            getApplication<Application>().stopService(intent)
        }
    }
}
