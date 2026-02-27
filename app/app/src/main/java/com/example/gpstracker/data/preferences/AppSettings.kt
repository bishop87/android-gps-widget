package com.example.gpstracker.data.preferences

/**
 * Chiavi per SettingsRepository, un'unica sorgente di verità per i nomi chiave.
 */
object SettingsKeys {
    const val DEVICE_NAME = "device_name"
    const val API_URL = "api_url"
    const val USERNAME = "username"
    const val PASSWORD = "password"
    const val TRACKING_ENABLED = "tracking_enabled"
    const val TRACKING_INTERVAL_SEC = "tracking_interval_sec"
    const val WIDGET_BACKGROUND_COLOR = "widget_background_color"
    const val OVERLAY_ENABLED = "overlay_enabled"
    const val OVERLAY_BACKGROUND_COLOR = "overlay_background_color"
}

/**
 * Modello dati per le impostazioni utente.
 */
data class AppSettings(
    val deviceName: String = "",
    val apiUrl: String = "",
    val username: String = "",
    val password: String = "",
    val trackingEnabled: Boolean = false,
    val trackingIntervalSec: Int = 300, // default 5 minuti
    val widgetBackgroundColor: Int = 0xCC1565C0.toInt(), // default Blue semi-trasparente
    val overlayEnabled: Boolean = false,
    val overlayBackgroundColor: Int = 0xCC1565C0.toInt() // default Blue semi-trasparente
)
