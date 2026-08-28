package com.embedsuite.app

import android.content.Intent

data class DeepLinkParams(
    val route: String?,
    val rfTab: Int?,
    val signalId: Long?,
    /** Changes on each intent so Compose re-runs navigation effects. */
    val token: Long = System.nanoTime()
) {
    companion object {
        fun from(intent: Intent?): DeepLinkParams? {
            if (intent == null) return null
            val route = intent.getStringExtra("navigate_to")
            val rfTab = intent.getIntExtra("rf_tab", -1).takeIf { it >= 0 }
            val signalId = intent.getLongExtra("signal_id", -1L).takeIf { it >= 0L }
            if (route == null && rfTab == null && signalId == null) return null
            return DeepLinkParams(route = route, rfTab = rfTab, signalId = signalId)
        }
    }
}
