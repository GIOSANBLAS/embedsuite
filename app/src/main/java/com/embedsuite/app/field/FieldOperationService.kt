package com.embedsuite.app.field

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.embedsuite.app.AppContainer
import com.embedsuite.app.MainActivity
import com.embedsuite.app.R
import com.embedsuite.app.data.SessionReportGenerator
import com.embedsuite.app.rf.RfFrequencyPresets
import kotlinx.coroutines.*
import java.io.File

class FieldOperationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private var captureJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopFieldMode()
                return START_NOT_STICKY
            }
            else -> startFieldMode()
        }
        return START_STICKY
    }

    private fun startFieldMode() {
        FieldOperationManager.setActive(true)
        val prefs = AppContainer.instance?.appPreferences
        val freq = prefs?.fieldFrequencyMhz ?: RfFrequencyPresets.DEFAULT
        val sessionName = FieldOperationManager.sessionName.ifBlank { "Campo" }
        FieldOperationManager.markSessionStarted(sessionName, freq)

        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Operación de campo", NotificationManager.IMPORTANCE_LOW)
            )
        }
        startForeground(NOTIFICATION_ID, buildNotification("EMBED // $sessionName @ $freq MHz"))

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "embed:field").apply {
            acquire(4 * 60 * 60 * 1000L)
        }

        AppContainer.instance?.locationTracker?.startTracking()

        captureJob = scope.launch {
            val container = AppContainer.instance ?: return@launch
            while (isActive) {
                container.connectionManager.setSubGhzFrequency(freq)
                container.connectionManager.startSubGhzRawCapture(15)
                delay(20_000)
            }
        }
    }

    private fun stopFieldMode() {
        releaseHardware()
        FieldOperationManager.setActive(false)
        scope.launch {
            val container = AppContainer.instance
            container?.exportHelper?.exportJson()
            val meta = FieldOperationManager.buildSessionMeta()
            if (meta != null && container != null) {
                container.sessionReportGenerator.generateFieldSessionReport(meta).onSuccess { file ->
                    FieldOperationManager.lastReportFile = file
                }
            } else {
                container?.sessionReportGenerator?.generateHtmlReport()?.onSuccess { file ->
                    FieldOperationManager.lastReportFile = file
                }
            }
            FieldOperationManager.clearSession()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Wake lock + GPS + job — debe liberarse también en onDestroy. */
    private fun releaseHardware() {
        captureJob?.cancel()
        captureJob = null
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                try {
                    lock.release()
                } catch (_: RuntimeException) {
                }
            }
        }
        wakeLock = null
        AppContainer.instance?.locationTracker?.stopTracking()
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, FieldOperationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("EMBED SUITE — Campo")
            .setContentText(text)
            .setContentIntent(openIntent)
            .addAction(0, "Detener", stopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        releaseHardware()
        FieldOperationManager.setActive(false)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.embedsuite.app.field.STOP"
        private const val CHANNEL_ID = "embed_field"
        private const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            val intent = Intent(context, FieldOperationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, FieldOperationService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}

object FieldOperationManager {
    private val _isActive = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isActiveFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isActive

    var sessionName: String = ""
        private set
    var sessionStartedAt: Long = 0L
        private set
    var sessionFrequencyMhz: String = RfFrequencyPresets.DEFAULT
        private set
    @Volatile
    var lastReportFile: File? = null

    var isActive: Boolean
        get() = _isActive.value
        private set(value) {
            _isActive.value = value
        }

    internal fun setActive(active: Boolean) {
        isActive = active
    }

    fun markSessionStarted(name: String, frequencyMhz: String) {
        sessionName = name.ifBlank { "Campo" }
        sessionFrequencyMhz = frequencyMhz.ifBlank { RfFrequencyPresets.DEFAULT }
        sessionStartedAt = System.currentTimeMillis()
        lastReportFile = null
    }

    fun buildSessionMeta(): SessionReportGenerator.FieldSessionMeta? {
        if (sessionStartedAt <= 0L) return null
        return SessionReportGenerator.FieldSessionMeta(
            name = sessionName.ifBlank { "Campo" },
            startedAt = sessionStartedAt,
            endedAt = System.currentTimeMillis(),
            frequencyMhz = sessionFrequencyMhz
        )
    }

    fun clearSession() {
        sessionStartedAt = 0L
    }

    fun start(context: Context, keepScreenOn: Boolean = false, name: String = "Campo") {
        context.applicationContext.let { ctx ->
            AppContainer.instance?.appPreferences?.fieldKeepScreenOn = keepScreenOn
            sessionName = name.trim().ifBlank { "Campo" }
            FieldOperationService.start(ctx)
        }
    }

    fun stop(context: Context) {
        FieldOperationService.stop(context.applicationContext)
    }
}
