package com.bishop87.gpstracker.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Helper centralizzato per la gestione dei permessi runtime.
 * Gestisce: FINE/COARSE LOCATION, BACKGROUND_LOCATION (API 29+), POST_NOTIFICATIONS (API 33+).
 */
object PermissionHelper {

    // Permessi base (richiesti su API 28+)
    val BASE_LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Background location: separato perché va richiesto DOPO i permessi base
    const val BACKGROUND_LOCATION = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    // Notifiche: richiesto solo su Android 13+
    val NOTIFICATION_PERMISSION: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else null

    /**
     * Verifica se la posizione fine (foreground) è disponibile.
     */
    fun hasFineLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Verifica se la posizione background è disponibile (API 29+).
     * Ritorna true su API < 29 (non necessaria).
     */
    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    /**
     * Verifica se il permesso notifiche è disponibile.
     * Ritorna true su API < 33 (non necessario).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    /**
     * Verifica se tutti i permessi necessari per il tracking in foreground sono concessi.
     */
    fun hasAllForegroundPermissions(context: Context): Boolean =
        hasFineLocationPermission(context) && hasNotificationPermission(context)

    /**
     * Verifica se tutti i permessi (incluso background) sono concessi.
     */
    fun hasAllPermissions(context: Context): Boolean =
        hasAllForegroundPermissions(context) && hasBackgroundLocationPermission(context)

    /**
     * Apre le impostazioni dell'app per la gestione manuale dei permessi.
     * Usato quando l'utente ha negato un permesso permanentemente.
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Controlla se l'app dovrebbe mostrare la rationale per il permesso.
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean =
        activity.shouldShowRequestPermissionRationale(permission)
}
