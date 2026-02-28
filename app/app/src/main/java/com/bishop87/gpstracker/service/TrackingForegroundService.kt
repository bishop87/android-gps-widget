package com.bishop87.gpstracker.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bishop87.gpstracker.GpsTrackerApp
import com.bishop87.gpstracker.R
import com.bishop87.gpstracker.data.repository.LocationRepository
import com.bishop87.gpstracker.data.repository.SettingsRepository
import com.bishop87.gpstracker.domain.usecase.GetCurrentLocationUseCase
import com.bishop87.gpstracker.domain.usecase.SendLocationUseCase
import com.bishop87.gpstracker.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Service per il tracking GPS in background.
 *
 * Gestisce:
 * - Timer interno per tracking periodico
 * - Notifica persistente obbligatoria (Android 8+)
 * - WakeLock parziale per prevenire sleep durante acquisizione
 * - Avvio/stop controllato da intent
 */
class TrackingForegroundService : Service() {

    private val tag = "TrackingFGService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var getCurrentLocation: GetCurrentLocationUseCase
    private lateinit var sendLocation: SendLocationUseCase

    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private var isRunning = false

    private val trackingRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            performTracking()
            val intervalMs = settingsRepository.getTrackingIntervalSec() * 1000L
            handler.postDelayed(this, intervalMs)
        }
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)

        val locationRepository = LocationRepository(this)
        getCurrentLocation = GetCurrentLocationUseCase(locationRepository)
        sendLocation = SendLocationUseCase(settingsRepository)

        acquireWakeLock()
        Log.d(tag, "Service creato")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            // Il sistema ha riavviato il servizio (START_STICKY)
            if (settingsRepository.isTrackingEnabled()) {
                startTracking()
            } else {
                stopSelf()
            }
        } else {
            when (intent.action) {
                ACTION_START -> startTracking()
                ACTION_STOP -> stopTracking()
            }
        }
        return START_STICKY // il sistema lo riavvia se viene killato
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        serviceScope.cancel()
        releaseWakeLock()
        Log.d(tag, "Service distrutto")
    }

    private fun startTracking() {
        if (isRunning) return
        isRunning = true
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.d(tag, "Tracking avviato, intervallo=${settingsRepository.getTrackingIntervalSec()}s")
        handler.post(trackingRunnable)
    }

    private fun stopTracking() {
        isRunning = false
        handler.removeCallbacks(trackingRunnable)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(tag, "Tracking fermato")
    }

    private fun performTracking() {
        serviceScope.launch {
            Log.d(tag, "Esecuzione tracking...")
            val locationResult = getCurrentLocation.execute()
            locationResult.onSuccess { location ->
                sendLocation.execute(location).onFailure { e ->
                    Log.e(tag, "Invio fallito: ${e.message}")
                }
            }.onFailure { e ->
                Log.e(tag, "Rilevazione posizione fallita: ${e.message}")
            }
        }
    }

    private fun buildNotification(): Notification {
        val mainIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GpsTrackerApp.CHANNEL_ID_TRACKING)
            .setContentTitle(getString(R.string.notification_tracking_title))
            .setContentText(getString(R.string.notification_tracking_text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(mainIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GpsTracker::TrackingWakeLock"
        ).also { it.acquire(10 * 60 * 1000L) /* max 10 min */ }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    companion object {
        const val ACTION_START = "com.bishop87.gpstracker.ACTION_START_TRACKING"
        const val ACTION_STOP = "com.bishop87.gpstracker.ACTION_STOP_TRACKING"
        private const val NOTIFICATION_ID = 1001
    }
}
