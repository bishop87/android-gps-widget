package com.bishop87.gpstracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bishop87.gpstracker.data.api.ApiClient
import com.bishop87.gpstracker.data.api.MapDataPoint
import com.bishop87.gpstracker.data.repository.SettingsRepository
import kotlinx.coroutines.launch

/**
 * ViewModel per la schermata Mappa.
 * Gestisce il fetch dei dati GPS da map_data2.php e l'ordinamento temporale.
 */
class MapViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepository = SettingsRepository(app)

    private val _points = MutableLiveData<List<MapDataPoint>>()
    val points: LiveData<List<MapDataPoint>> get() = _points

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> get() = _error

    /**
     * Esegue il fetch dei punti GPS dall'endpoint configurato.
     * @param dateFrom Data/ora inizio nel formato "YYYY-MM-DD HH:mm" (può essere null)
     * @param dateTo Data/ora fine nel formato "YYYY-MM-DD HH:mm" (può essere null)
     */
    fun fetchMapData(
        dateFrom: String? = null,
        dateTo: String? = null
    ) {
        val settings = settingsRepository.loadSettings()

        val mapApiUrl = settings.mapApiUrl
        if (mapApiUrl.isBlank()) {
            _error.value = "URL API Mappa non configurato. Vai nelle Impostazioni."
            return
        }

        val username = settings.username
        val password = settings.password

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val service = ApiClient.create(mapApiUrl, username, password)
                val deviceParam = if (settings.deviceName.isBlank()) null else settings.deviceName
                val response = service.getMapData(
                    url = mapApiUrl,
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    device = deviceParam
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    when {
                        body == null -> _error.value = "Risposta API vuota"
                        !body.success -> _error.value = body.error ?: "Errore API sconosciuto"
                        body.data.isNullOrEmpty() -> {
                            _points.value = emptyList()
                            _error.value = "Nessun dato GPS trovato per il filtro selezionato"
                        }
                        else -> {
                            // L'API restituisce i dati in ordine DESC.
                            // Riordiniamo in ASC per timestamp prima di disegnare la polyline.
                            val sorted = body.data.sortedBy { it.timestamp ?: "" }
                            _points.value = sorted
                        }
                    }
                } else {
                    _error.value = "Errore HTTP ${response.code()}: ${response.message()}"
                }
            } catch (e: java.net.UnknownHostException) {
                _error.value = "Nessuna connessione di rete"
            } catch (e: java.net.SocketTimeoutException) {
                _error.value = "Timeout di rete"
            } catch (e: Exception) {
                _error.value = "Errore: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
