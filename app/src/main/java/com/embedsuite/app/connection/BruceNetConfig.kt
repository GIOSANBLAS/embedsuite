package com.embedsuite.app.connection

import android.content.Context
import com.embedsuite.app.R

/** Credenciales del AP BruceNet — valores en `strings.xml` (`bruce_net_*`). */
object BruceNetConfig {
    fun ssid(context: Context): String = context.getString(R.string.bruce_net_ssid)

    fun password(context: Context): String = context.getString(R.string.bruce_net_password)

    fun defaultHost(context: Context): String = context.getString(R.string.bruce_net_host)
}
