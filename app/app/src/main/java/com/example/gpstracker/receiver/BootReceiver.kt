package com.example.gpstracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.gpstracker.data.repository.SettingsRepository
import com.example.gpstracker.service.TrackingForegroundService

/**
 * BroadcastReceiver per il ripristino del tracking dopo il riavvio del device.
 * Gestisce BOOT_COMPLETED e QUICKBOOT_POWERON (Huawei/altri OEM).
 */
class BootReceiver : BroadcastReceiver() {

    private val tag = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val receivedAction = intent.action
        if (receivedAction != Intent.ACTION_BOOT_COMPLETED &&
            receivedAction != "android.intent.action.QUICKBOOT_POWERON") {
            return
        }

        Log.d(tag, "Boot completato — verifica stato tracking")

        val settingsRepository = SettingsRepository(context)
        if (settingsRepository.isTrackingEnabled()) {
            Log.d(tag, "Tracking era abilitato — riavvio Foreground Service")
            val serviceIntent = Intent(context, TrackingForegroundService::class.java).apply {
                setAction(TrackingForegroundService.ACTION_START)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            Log.d(tag, "Tracking non abilitato — nessuna azione al boot")
        }

        if (settingsRepository.isOverlayEnabled()) {
            val canDraw = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(context)
            if (canDraw) {
                Log.d(tag, "Overlay abilitato e permesso concesso — avvio Overlay Service")
                context.startService(Intent(context, com.example.gpstracker.service.OverlayService::class.java))
            } else {
                Log.w(tag, "Overlay abilitato ma permesso negato al boot")
            }
        }
    }
}
