package com.example.gpstracker.domain.usecase

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.gpstracker.service.TrackingForegroundService

/**
 * Use case per avviare il tracking in background (Foreground Service).
 */
class StartTrackingUseCase(private val context: Context) {

    fun execute() {
        val intent = Intent(context, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}

/**
 * Use case per fermare il tracking in background.
 */
class StopTrackingUseCase(private val context: Context) {

    fun execute() {
        val intent = Intent(context, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }
}
