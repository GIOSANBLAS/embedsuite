package com.embedsuite.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.embedsuite.app.connection.TransportType
import com.embedsuite.app.core.SoundFeedback
import com.embedsuite.app.ui.theme.EMBEDSUITETheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer
    private val activityJob = SupervisorJob()
    private val activityScope = CoroutineScope(activityJob + Dispatchers.Main)
    private var deepLinkState by mutableStateOf<DeepLinkParams?>(null)

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbSerialManager.ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device = extractUsbDevice(intent)
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.let {
                                Toast.makeText(context, "Permiso USB concedido", Toast.LENGTH_SHORT).show()
                                activityScope.launch {
                                    container.connectionManager.connect(TransportType.USB)
                                }
                            }
                        } else {
                            Toast.makeText(context, "Permiso USB denegado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    Toast.makeText(context, "Dispositivo USB Conectado", Toast.LENGTH_SHORT).show()
                    val device = extractUsbDevice(intent) ?: return
                    if (!::container.isInitialized) return
                    if (container.usbSerialManager.tienePermiso(device)) {
                        activityScope.launch {
                            container.connectionManager.connect(TransportType.USB)
                        }
                    } else {
                        container.usbSerialManager.solicitarPermiso(device)
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Toast.makeText(context, "Dispositivo USB Desconectado", Toast.LENGTH_SHORT).show()
                    if (!::container.isInitialized) return
                    activityScope.launch {
                        container.connectionManager.disconnect()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        container = (application as EmbedApplication).container

        val filter = IntentFilter().apply {
            addAction(UsbSerialManager.ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

        ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        deepLinkState = DeepLinkParams.from(intent)

        setContent {
            EMBEDSUITETheme {
                MainScreen(
                    container = container,
                    deepLink = deepLinkState
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkState = DeepLinkParams.from(intent)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(usbReceiver)
        } catch (_: IllegalArgumentException) {
            // ya no registrado
        }
        if (::container.isInitialized) {
            container.locationTracker.stopTracking()
            container.wirelessScanner.stopBleScan()
            // No bloquear el hilo UI (ANR). Desconexión con timeout en IO.
            val cm = container.connectionManager
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                withTimeoutOrNull(2_000L) { cm.disconnect() }
            }
        }
        SoundFeedback.release()
        activityJob.cancel()
        super.onDestroy()
    }

    private fun extractUsbDevice(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }
}
