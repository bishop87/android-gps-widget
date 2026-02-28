package com.bishop87.gpstracker.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bishop87.gpstracker.data.repository.LocationRepository
import com.bishop87.gpstracker.data.repository.SettingsRepository
import com.bishop87.gpstracker.domain.usecase.GetCurrentLocationUseCase
import com.bishop87.gpstracker.domain.usecase.SendLocationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver per i tap sul widget.
 * Esegue il ciclo: IDLE → ACQUIRING → SENDING → SUCCESS/ERROR
 * Usa coroutine con SupervisorJob per non bloccare il main thread.
 */
class GpsWidgetReceiver : BroadcastReceiver() {

    private val tag = "GpsWidgetReceiver"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != GpsWidgetProvider.ACTION_WIDGET_TAP) return

        val widgetId = intent.getIntExtra(
            GpsWidgetProvider.EXTRA_WIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        Log.d(tag, "Tap widget id=$widgetId")

        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Usa goAsync() per permettere operazioni asincrone nel BroadcastReceiver
        val pendingResult = goAsync()

        scope.launch {
            try {
                // Step 1: ACQUIRING
                GpsWidgetProvider.updateWidget(context, appWidgetManager, widgetId, WidgetState.ACQUIRING)

                val locationRepo = LocationRepository(context)
                val settingsRepo = SettingsRepository(context)
                val getLocation = GetCurrentLocationUseCase(locationRepo)
                val sendLocationUseCase = SendLocationUseCase(settingsRepo)

                val locationResult = getLocation.execute(timeoutMs = 20_000L)

                locationResult.onSuccess { location ->
                    // Step 2: SENDING
                    GpsWidgetProvider.updateWidget(context, appWidgetManager, widgetId, WidgetState.SENDING)

                    sendLocationUseCase.execute(location).onSuccess {
                        GpsWidgetProvider.updateWidget(context, appWidgetManager, widgetId, WidgetState.SUCCESS)
                        Log.d(tag, "Widget: invio riuscito")
                    }.onFailure { e ->
                        GpsWidgetProvider.updateWidget(context, appWidgetManager, widgetId, WidgetState.ERROR)
                        Log.e(tag, "Widget: invio fallito: ${e.message}")
                    }
                }.onFailure { e ->
                    GpsWidgetProvider.updateWidget(context, appWidgetManager, widgetId, WidgetState.ERROR)
                    Log.e(tag, "Widget: GPS fallito: ${e.message}")
                }

                // Torna a IDLE dopo 3 secondi
                kotlinx.coroutines.delay(3000L)
                GpsWidgetProvider.updateWidget(context, appWidgetManager, widgetId, WidgetState.IDLE)

            } finally {
                pendingResult.finish()
            }
        }
    }
}
