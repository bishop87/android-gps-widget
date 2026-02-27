package com.example.gpstracker.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository per la rilevazione della posizione GPS.
 * Usa FusedLocationProviderClient con:
 * - timeout configurabile (default 20s)
 * - fallback a posizione coarse se fine non disponibile
 * - scarto posizioni con accuracy > soglia configurabile
 */
class LocationRepository(context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val tag = "LocationRepository"

    /**
     * Richiede la posizione corrente con timeout.
     * Ritorna null se il timeout scade o GPS non disponibile.
     *
     * @param timeoutMs timeout in ms (default 20_000 = 20s)
     * @param maxAccuracyMeters scarta posizioni con accuracy peggiore di questo valore
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(
        timeoutMs: Long = 20_000L,
        maxAccuracyMeters: Float = 50f
    ): LocationData? {

        // Prima tenta di usare l'ultima posizione nota (veloce), ma solo se recente (< 30s)
        val lastKnown = tryGetLastKnownLocation()
        val now = System.currentTimeMillis()
        if (lastKnown != null && 
            (now - lastKnown.gpsTime) < 30_000L && 
            lastKnown.accuracy <= maxAccuracyMeters) {
            Log.d(tag, "Usata ultima posizione nota recente: accuracy=${lastKnown.accuracy}m, age=${(now - lastKnown.gpsTime)/1000}s")
            return lastKnown
        }

        // Richiesta posizione fresca con timeout
        val freshLocation = withTimeoutOrNull(timeoutMs) {
            requestFreshLocation()
        }

        if (freshLocation == null) {
            Log.w(tag, "Timeout GPS dopo ${timeoutMs}ms — fallback a ultima posizione coarse")
            return lastKnown // può essere null se nessuna posizione disponibile
        }

        if (freshLocation.accuracy > maxAccuracyMeters) {
            Log.w(tag, "Accuracy ${freshLocation.accuracy}m supera soglia ${maxAccuracyMeters}m")
            // Ritorna comunque ma loggato come subottimale
        }

        return freshLocation
    }

    @SuppressLint("MissingPermission")
    private suspend fun tryGetLastKnownLocation(): LocationData? =
        suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        cont.resume(location.toLocationData())
                    } else {
                        cont.resume(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(tag, "Impossibile ottenere ultima posizione: ${e.message}")
                    cont.resume(null)
                }
        }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(): LocationData =
        suspendCancellableCoroutine { cont ->
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
                .setMaxUpdates(1)
                .setWaitForAccurateLocation(false)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fusedClient.removeLocationUpdates(this)
                    val location = result.lastLocation
                    if (location != null) {
                        Log.d(tag, "Posizione fresca: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}m")
                        cont.resume(location.toLocationData())
                    } else {
                        cont.resumeWithException(Exception("LocationResult vuoto"))
                    }
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    if (!availability.isLocationAvailable) {
                        Log.w(tag, "GPS non disponibile")
                    }
                }
            }

            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

            cont.invokeOnCancellation {
                fusedClient.removeLocationUpdates(callback)
            }
        }

    private fun Location.toLocationData() = LocationData(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        timestamp = System.currentTimeMillis(),
        gpsTime = time
    )
}
