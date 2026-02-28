package com.bishop87.gpstracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Helper per la gestione dell'ottimizzazione batteria.
 * Permette di verificare e richiedere all'utente di escludere l'app dalle restrizioni energetiche.
 */
object BatteryOptimizationHelper {

    /**
     * Ritorna true se l'app è attualmente esclusa dall'ottimizzazione batteria.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Apre il dialog di sistema per richiedere l'esclusione dall'ottimizzazione batteria.
     * Richiede il permesso REQUEST_IGNORE_BATTERY_OPTIMIZATIONS nel manifest.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(context)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    /**
     * Apre la schermata generale delle impostazioni di ottimizzazione batteria.
     * Alternativa quando ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS non è disponibile.
     */
    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Ritorna il livello della batteria in percentuale (0-100).
     */
    fun getBatteryLevel(context: Context): Int {
        val batteryStatus: android.content.Intent? = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            100 // Fallback
        }
    }
}
