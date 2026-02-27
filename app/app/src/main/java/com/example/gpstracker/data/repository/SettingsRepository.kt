package com.example.gpstracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.gpstracker.data.preferences.AppSettings
import com.example.gpstracker.data.preferences.SettingsKeys

/**
 * Repository per le impostazioni dell'app.
 * Usa EncryptedSharedPreferences per salvare in modo sicuro le credenziali API.
 */
class SettingsRepository(val context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "gps_tracker_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun loadSettings(): AppSettings {
        return AppSettings(
            deviceName = prefs.getString(SettingsKeys.DEVICE_NAME, "") ?: "",
            apiUrl = prefs.getString(SettingsKeys.API_URL, "") ?: "",
            username = prefs.getString(SettingsKeys.USERNAME, "") ?: "",
            password = prefs.getString(SettingsKeys.PASSWORD, "") ?: "",
            trackingEnabled = prefs.getBoolean(SettingsKeys.TRACKING_ENABLED, false),
            trackingIntervalSec = prefs.getInt(SettingsKeys.TRACKING_INTERVAL_SEC, 300),
            widgetBackgroundColor = prefs.getInt(SettingsKeys.WIDGET_BACKGROUND_COLOR, 0xCC1565C0.toInt()),
            overlayEnabled = prefs.getBoolean(SettingsKeys.OVERLAY_ENABLED, false),
            overlayBackgroundColor = prefs.getInt(SettingsKeys.OVERLAY_BACKGROUND_COLOR, 0xCC1565C0.toInt())
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit().apply {
            putString(SettingsKeys.DEVICE_NAME, settings.deviceName)
            putString(SettingsKeys.API_URL, settings.apiUrl)
            putString(SettingsKeys.USERNAME, settings.username)
            putString(SettingsKeys.PASSWORD, settings.password)
            putBoolean(SettingsKeys.TRACKING_ENABLED, settings.trackingEnabled)
            putInt(SettingsKeys.TRACKING_INTERVAL_SEC, settings.trackingIntervalSec)
            putInt(SettingsKeys.WIDGET_BACKGROUND_COLOR, settings.widgetBackgroundColor)
            putBoolean(SettingsKeys.OVERLAY_ENABLED, settings.overlayEnabled)
            putInt(SettingsKeys.OVERLAY_BACKGROUND_COLOR, settings.overlayBackgroundColor)
            apply()
        }
    }

    fun getWidgetBackgroundColor(): Int =
        prefs.getInt(SettingsKeys.WIDGET_BACKGROUND_COLOR, 0xCC1565C0.toInt())

    fun getOverlayBackgroundColor(): Int =
        prefs.getInt(SettingsKeys.OVERLAY_BACKGROUND_COLOR, 0xCC1565C0.toInt())

    fun isTrackingEnabled(): Boolean =
        prefs.getBoolean(SettingsKeys.TRACKING_ENABLED, false)

    fun setTrackingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(SettingsKeys.TRACKING_ENABLED, enabled).apply()
    }
    
    fun isOverlayEnabled(): Boolean =
        prefs.getBoolean(SettingsKeys.OVERLAY_ENABLED, false)

    fun setOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(SettingsKeys.OVERLAY_ENABLED, enabled).apply()
    }

    fun getTrackingIntervalSec(): Int =
        prefs.getInt(SettingsKeys.TRACKING_INTERVAL_SEC, 300)

    fun getApiUrl(): String =
        prefs.getString(SettingsKeys.API_URL, "") ?: ""

    fun getUsername(): String =
        prefs.getString(SettingsKeys.USERNAME, "") ?: ""

    fun getPassword(): String =
        prefs.getString(SettingsKeys.PASSWORD, "") ?: ""

    fun getDeviceName(): String =
        prefs.getString(SettingsKeys.DEVICE_NAME, "") ?: ""
}
