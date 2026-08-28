package com.embedsuite.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Allows `adb shell am broadcast -a com.embedsuite.app.START_HEADLESS` from automation. */
class HeadlessServiceLauncher : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val serviceIntent = Intent(context, EmbedHeadlessService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
