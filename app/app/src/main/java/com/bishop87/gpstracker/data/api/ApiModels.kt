package com.bishop87.gpstracker.data.api

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

/**
 * Singolo punto GPS restituito da map_data2.php.
 */
data class MapDataPoint(
    val device: String?,
    val timestamp: String?,
    val latitude: String?,
    val longitude: String?,
    val accuracy: String?,
    val battery: String?
)

/**
 * Risposta completa di map_data2.php.
 */
data class MapDataResponse(
    val success: Boolean,
    val count: Int? = null,
    val error: String? = null,
    val data: List<MapDataPoint>? = null
)
