package com.embedsuite.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import android.app.PendingIntent
import android.content.Intent
import com.embedsuite.app.MainActivity
import com.embedsuite.app.R

object EmbedNotificationHelper {

    private const val CHANNEL_ID = "embed_connection"
    private const val RF_CHANNEL_ID = "embed_rf_automation"
    private const val NOTIFICATION_ID = 1001
    private const val RF_NOTIFICATION_ID = 1002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "EMBED SUITE Conexión",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Estado de conexión con T-Embed CC1101"
        }
        manager.createNotificationChannel(channel)
    }

    fun ensureRfChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(RF_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            RF_CHANNEL_ID,
            "EMBED RF Automatizaciones",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertas cuando una regla RF coincide con una captura"
        }
        manager.createNotificationChannel(channel)
    }

    fun notifyRfMatch(context: Context, ruleName: String, detail: String, signalId: Long? = null) {
        ensureRfChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "rf")
            putExtra("rf_tab", 1)
            signalId?.let { putExtra("signal_id", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (ruleName + detail).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, RF_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("RF: $ruleName")
            .setContentText(detail)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(RF_NOTIFICATION_ID + ruleName.hashCode().and(0xFFFF), notification)
    }

    fun notifyConnection(context: Context, title: String, message: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }
}
