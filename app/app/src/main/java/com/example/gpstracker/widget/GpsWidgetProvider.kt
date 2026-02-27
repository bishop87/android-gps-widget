package com.example.gpstracker.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.gpstracker.R

/**
 * Provider del widget home screen.
 * Gestisce aggiornamenti e click sul bottone del widget.
 */
class GpsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId, WidgetState.IDLE)
        }
    }

    companion object {
        const val ACTION_WIDGET_TAP = "com.example.gpstracker.ACTION_WIDGET_TAP"
        const val EXTRA_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            state: WidgetState
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_gps)

            // Configura testo stato
            val statusText = when (state) {
                WidgetState.IDLE -> context.getString(R.string.widget_label)
                WidgetState.ACQUIRING -> context.getString(R.string.status_acquiring)
                WidgetState.SENDING -> context.getString(R.string.status_sending)
                WidgetState.SUCCESS -> context.getString(R.string.status_success)
                WidgetState.ERROR -> "Errore"
            }
            views.setTextViewText(R.id.tv_widget_status, statusText)

            // Carica e applica il colore di sfondo dalle impostazioni
            val settingsRepo = com.example.gpstracker.data.repository.SettingsRepository(context)
            val bgColor = settingsRepo.getWidgetBackgroundColor()
            views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)

            // Configura PendingIntent per il tap — debounce: non inviare se già in corso
            if (state == WidgetState.IDLE || state == WidgetState.SUCCESS || state == WidgetState.ERROR) {
                val tapIntent = Intent(context, GpsWidgetReceiver::class.java).apply {
                    action = ACTION_WIDGET_TAP
                    putExtra(EXTRA_WIDGET_ID, widgetId)
                }
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    widgetId,
                    tapIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_widget_send, pendingIntent)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}

enum class WidgetState { IDLE, ACQUIRING, SENDING, SUCCESS, ERROR }
