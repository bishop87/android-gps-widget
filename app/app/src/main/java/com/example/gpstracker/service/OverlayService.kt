package com.example.gpstracker.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import com.example.gpstracker.R
import com.example.gpstracker.data.repository.LocationRepository
import com.example.gpstracker.data.repository.SettingsRepository
import com.example.gpstracker.domain.usecase.GetCurrentLocationUseCase
import com.example.gpstracker.domain.usecase.SendLocationUseCase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Servizio per mostrare un bottone in overlay sullo schermo.
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var params: WindowManager.LayoutParams
    
    private lateinit var btnSend: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var getLocation: GetCurrentLocationUseCase
    private lateinit var sendLocation: SendLocationUseCase

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        val locationRepo = LocationRepository(this)
        val settingsRepo = SettingsRepository(this)
        getLocation = GetCurrentLocationUseCase(locationRepo)
        sendLocation = SendLocationUseCase(settingsRepo)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val themeContext = android.view.ContextThemeWrapper(this, R.style.Theme_GpsTracker)
        overlayView = LayoutInflater.from(themeContext).inflate(R.layout.view_overlay_button, null)

        btnSend = overlayView.findViewById(R.id.btnOverlaySend)
        progressBar = overlayView.findViewById(R.id.progressOverlay)

        // Carica e applica il colore personalizzato
        val overlayColor = settingsRepo.getOverlayBackgroundColor()
        btnSend.backgroundTintList = android.content.res.ColorStateList.valueOf(overlayColor)

        val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        windowManager.addView(overlayView, params)
        setupTouchListener()
    }

    private fun setupTouchListener() {
        btnSend.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private var isMoved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoved = false
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoved) {
                            v.performClick()
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        
                        // Soglia di spostamento per distinguere tra un click e un drag
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isMoved = true
                            params.x = initialX + dx.toInt()
                            params.y = initialY + dy.toInt()
                            windowManager.updateViewLayout(overlayView, params)
                        }
                        return true
                    }
                }
                return false
            }
        })

        btnSend.setOnClickListener {
            performLocationSend()
        }
    }

    private fun performLocationSend() {
        if (progressBar.visibility == View.VISIBLE) return // Già in corso

        serviceScope.launch {
            setUiState(isWorking = true)
            
            val locationResult = getLocation.execute(timeoutMs = 20_000L)

            locationResult.onSuccess { location ->
                sendLocation.execute(location).onSuccess {
                    showToastOnMain("Posizione inviata (Overlay)")
                    setUiState(isWorking = false, success = true)
                }.onFailure { e ->
                    showToastOnMain("Errore invio: ${e.message}")
                    setUiState(isWorking = false, success = false)
                }
            }.onFailure { e ->
                showToastOnMain("Errore GPS: ${e.message}")
                setUiState(isWorking = false, success = false)
            }
        }
    }

    private suspend fun setUiState(isWorking: Boolean, success: Boolean? = null) {
        withContext(Dispatchers.Main) {
            progressBar.visibility = if (isWorking) View.VISIBLE else View.GONE
            
            if (isWorking) {
                btnSend.setColorFilter(Color.GRAY)
            } else {
                when (success) {
                    true -> btnSend.setColorFilter(Color.GREEN)
                    false -> btnSend.setColorFilter(Color.RED)
                    null -> btnSend.clearColorFilter()
                }
                
                // Torna al colore normale dopo 3 secondi se c'è stato un esito
                if (success != null) {
                    overlayView.postDelayed({
                        btnSend.clearColorFilter()
                    }, 3000)
                }
            }
        }
    }

    private suspend fun showToastOnMain(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@OverlayService, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "com.example.gpstracker.ACTION_START_OVERLAY"
        const val ACTION_STOP = "com.example.gpstracker.ACTION_STOP_OVERLAY"
    }
}
