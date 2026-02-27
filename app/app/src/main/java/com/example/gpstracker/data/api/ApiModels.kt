package com.example.gpstracker.data.api

/**
 * Modello dati per inviare la posizione all'API.
 * Allineato con i nomi attesi dal PHP: timestamp, latitude, longitude, accuracy, device, battery.
 */
data class LocationPayload(
    val device: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val battery: Int
)

/**
 * Risposta generica dell'API.
 */
data class ApiResponse(
    val status: String,
    val message: String? = null
)
