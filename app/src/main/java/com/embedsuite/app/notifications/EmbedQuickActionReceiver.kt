package com.embedsuite.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.embedsuite.app.EmbedApplication
import kotlinx.coroutines.launch

/** Acciones rápidas desde la notificación de conexión (solo Bruce CLI). */
class EmbedQuickActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pending = goAsync()
        val app = context.applicationContext as? EmbedApplication ?: run {
            pending.finish()
            return
        }
        val cm = app.container.connectionManager
        app.container.appScope.launch {
            when (action) {
                ACTION_REFRESH -> cm.refreshSystemInfo()
                ACTION_NAV_MENU -> cm.sendBruceCliLine("nav esc")
                ACTION_DISCONNECT -> cm.disconnect()
            }
            pending.finish()
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.embedsuite.app.action.QUICK_REFRESH"
        const val ACTION_NAV_MENU = "com.embedsuite.app.action.QUICK_NAV_MENU"
        const val ACTION_DISCONNECT = "com.embedsuite.app.action.QUICK_DISCONNECT"
    }
}
