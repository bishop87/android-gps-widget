package com.example.gpstracker.domain.usecase

import android.util.Log
import com.example.gpstracker.data.repository.LocationData
import com.example.gpstracker.data.repository.LocationRepository

/**
 * Use case per ottenere la posizione corrente del dispositivo.
 * Logica pura, agnostica rispetto alla UI.
 */
class GetCurrentLocationUseCase(
    private val locationRepository: LocationRepository
) {
    private val tag = "GetCurrentLocationUseCase"

    /**
     * Esegue la rilevazione della posizione.
     * @return LocationData o null se non disponibile/timeout
     */
    suspend fun execute(
        timeoutMs: Long = 20_000L,
        maxAccuracyMeters: Float = 50f
    ): Result<LocationData> {
        return try {
            val location = locationRepository.getCurrentLocation(timeoutMs, maxAccuracyMeters)
            if (location != null) {
                Log.d(tag, "Posizione rilevata: lat=${location.latitude}, lon=${location.longitude}")
                Result.success(location)
            } else {
                Log.w(tag, "Nessuna posizione disponibile")
                Result.failure(Exception("Posizione GPS non disponibile"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Errore rilevazione posizione: ${e.message}")
            Result.failure(e)
        }
    }
}
