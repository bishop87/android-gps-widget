package com.example.gpstracker.data.repository

/**
 * Dati di localizzazione rilevati.
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val gpsTime: Long = 0
)
