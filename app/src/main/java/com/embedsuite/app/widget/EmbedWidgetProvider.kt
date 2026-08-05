package com.embedsuite.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.embedsuite.app.AppContainer
import com.embedsuite.app.MainActivity
import com.embedsuite.app.R
import com.embedsuite.app.connection.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmbedWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_RX_15S, ACTION_TX_FAV -> {
                if (!WidgetStateStore.isValidActionToken(
                        context,
                        intent.getStringExtra(WidgetStateStore.EXTRA_ACTION_TOKEN)
                    )
                ) {
                    return
                }
            }
        }
        when (intent.action) {
            ACTION_RX_15S -> {
                scope.launch {
                    try {
                        val cm = AppContainer.instance?.connectionManager
                        if (cm == null) {
                            toast(context, context.getString(R.string.widget_tx_no_app))
                            return@launch
                        }
                        if (cm.connectionState.value !is ConnectionState.Connected) {
                            toast(context, context.getString(R.string.widget_tx_offline))
                            return@launch
                        }
                        cm.startSubGhzRawCapture(15).onFailure {
                            toast(context, it.message ?: context.getString(R.string.widget_tx_fail))
                        }
                    } catch (e: Exception) {
                        toast(context, e.message ?: context.getString(R.string.widget_tx_fail))
                    }
                    updateAllWidgets(context)
                }
            }
            ACTION_TX_FAV -> {
                scope.launch {
                    try {
                        val container = AppContainer.instance
                        if (container == null) {
                            toast(context, context.getString(R.string.widget_tx_no_app))
                            return@launch
                        }
                        val link = container.connectionManager.connectionState.value
                        if (link !is ConnectionState.Connected) {
                            toast(context, context.getString(R.string.widget_tx_offline))
                            return@launch
                        }
                        val fav = container.signalRepository.getFavoriteRf(1).firstOrNull()
                        if (fav == null) {
                            toast(context, context.getString(R.string.widget_tx_no_fav))
                            return@launch
                        }
                        WidgetStateStore.updateFavoriteLabel(
                            context,
                            fav.label.ifBlank { fav.protocol.ifBlank { fav.name } }
                        )
                        container.rfReplayEngine.replay(fav).fold(
                            onSuccess = {
                                toast(
                                    context,
                                    context.getString(
                                        R.string.widget_tx_ok,
                                        fav.label.ifBlank { fav.protocol }
                                    )
                                )
                            },
                            onFailure = {
                                toast(context, it.message ?: context.getString(R.string.widget_tx_fail))
                            }
                        )
                    } catch (e: Exception) {
                        toast(context, e.message ?: context.getString(R.string.widget_tx_fail))
                    }
                    updateAllWidgets(context)
                }
            }
        }
    }

    companion object {
        const val ACTION_RX_15S = "com.embedsuite.app.widget.RX_15S"
        const val ACTION_TX_FAV = "com.embedsuite.app.widget.TX_FAV"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private suspend fun toast(context: Context, msg: String) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, EmbedWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_embed)
            val container = AppContainer.instance

            val linkText = when (val state = container?.connectionManager?.connectionState?.value) {
                is ConnectionState.Connected -> context.getString(R.string.widget_link_ok, state.type.name)
                ConnectionState.Connecting -> context.getString(R.string.widget_sync)
                is ConnectionState.Error -> context.getString(R.string.widget_err)
                else -> context.getString(R.string.widget_offline)
            }
            views.setTextViewText(R.id.widget_status, linkText)

            val lastFreq = WidgetStateStore.lastFrequency(context)
            val lastProtocol = WidgetStateStore.lastProtocol(context)
            views.setTextViewText(
                R.id.widget_last_freq,
                context.getString(R.string.widget_last_freq, lastProtocol, lastFreq)
            )

            val favLabel = WidgetStateStore.favoriteLabel(context)
            views.setTextViewText(
                R.id.widget_fav,
                if (favLabel.isNotBlank()) {
                    context.getString(R.string.widget_fav_label, favLabel)
                } else {
                    context.getString(R.string.widget_fav_empty)
                }
            )

            scope.launch {
                val count = container?.signalRepository?.count() ?: 0
                val fav = container?.signalRepository?.getFavoriteRf(1)?.firstOrNull()
                if (fav != null) {
                    WidgetStateStore.updateFavoriteLabel(
                        context,
                        fav.label.ifBlank { fav.protocol.ifBlank { fav.name } }
                    )
                }
                views.setTextViewText(R.id.widget_signals, context.getString(R.string.widget_signals, count))
                manager.updateAppWidget(widgetId, views)
            }

            val openIntent = activityIntent(context, null)
            views.setOnClickPendingIntent(R.id.widget_container, openIntent)

            val rfIntent = activityIntent(context, "rf")
            views.setOnClickPendingIntent(R.id.widget_btn_rf, rfIntent)

            val token = WidgetStateStore.ensureActionToken(context)
            val rxIntent = PendingIntent.getBroadcast(
                context, 2,
                Intent(context, EmbedWidgetProvider::class.java)
                    .setAction(ACTION_RX_15S)
                    .putExtra(WidgetStateStore.EXTRA_ACTION_TOKEN, token),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_rx, rxIntent)

            val txFavIntent = PendingIntent.getBroadcast(
                context, 3,
                Intent(context, EmbedWidgetProvider::class.java)
                    .setAction(ACTION_TX_FAV)
                    .putExtra(WidgetStateStore.EXTRA_ACTION_TOKEN, token),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_tx_fav, txFavIntent)

            manager.updateAppWidget(widgetId, views)
        }

        private fun activityIntent(context: Context, route: String?): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                route?.let { putExtra("navigate_to", it) }
            }
            return PendingIntent.getActivity(
                context,
                route?.hashCode() ?: 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
