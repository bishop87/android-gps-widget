package com.example.gpstracker.domain.usecase

import android.util.Log
import com.example.gpstracker.data.api.ApiClient
import com.example.gpstracker.data.api.LocationPayload
import com.example.gpstracker.data.repository.LocationData
import com.example.gpstracker.data.repository.SettingsRepository

/**
 * Use case per inviare la posizione al backend remoto.
 * Costruisce il payload con il nome device dalle impostazioni e spedisce via Retrofit.
 */
class SendLocationUseCase(
    private val settingsRepository: SettingsRepository
) {
    private val tag = "SendLocationUseCase"

    /**
     * Invia la posizione all'API.
     * @param location LocationData da inviare
     * @return Result<Unit> — successo o failure con messaggio
     */
    suspend fun execute(location: LocationData): Result<Unit> {
        val apiUrl = settingsRepository.getApiUrl()
        val username = settingsRepository.getUsername()
        val password = settingsRepository.getPassword()
        val deviceName = settingsRepository.getDeviceName()

        if (apiUrl.isBlank()) {
            return Result.failure(Exception("URL API non configurato"))
        }

        if (!apiUrl.startsWith("https://")) {
            return Result.failure(Exception("L'URL API deve usare HTTPS"))
        }

        return try {
            val apiService = ApiClient.create(apiUrl, username, password)
            val batteryLevel = com.example.gpstracker.util.BatteryOptimizationHelper.getBatteryLevel(
                settingsRepository.context
            )

            Log.d(tag, "Invio posizione a $apiUrl — device='$deviceName' battery=$batteryLevel%")
            
            val response = apiService.sendLocation(
                url = apiUrl,
                device = deviceName,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                timestamp = location.timestamp,
                battery = batteryLevel
            )

            if (response.isSuccessful) {
                Log.d(tag, "Invio riuscito: ${response.code()}")
                Result.success(Unit)
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.w(tag, "Errore risposta API: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "Errore invio: ${e.message}")
            Result.failure(e)
        }
    }
}
