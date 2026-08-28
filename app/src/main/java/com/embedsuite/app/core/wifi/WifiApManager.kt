package com.embedsuite.app.core.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Gestión de conexión al AP del T-Embed (192.168.4.1 típico). */
class WifiApManager(context: Context) {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _boundToWifi = MutableStateFlow(false)
    val boundToWifi: StateFlow<Boolean> = _boundToWifi.asStateFlow()

    private var callback: ConnectivityManager.NetworkCallback? = null

    fun bindToWifiTransport() {
        if (callback != null) return
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                cm.bindProcessToNetwork(network)
                _boundToWifi.value = true
            }

            override fun onLost(network: Network) {
                cm.bindProcessToNetwork(null)
                _boundToWifi.value = false
            }
        }
        cm.registerNetworkCallback(request, callback!!)
    }

    fun unbind() {
        callback?.let { cm.unregisterNetworkCallback(it) }
        callback = null
        cm.bindProcessToNetwork(null)
        _boundToWifi.value = false
    }

    companion object {
        const val DEFAULT_HOST = "192.168.4.1"
        const val DEFAULT_TEHLINK_PORT = 8888
    }
}
