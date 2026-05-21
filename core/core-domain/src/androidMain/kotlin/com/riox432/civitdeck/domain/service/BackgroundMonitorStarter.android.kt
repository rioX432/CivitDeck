package com.riox432.civitdeck.domain.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.riox432.civitdeck.util.Logger

actual class BackgroundMonitorStarter(
    private val context: Context,
) {
    actual fun startMonitoring(promptId: String, baseUrl: String, wsScheme: String) {
        val intent = Intent().apply {
            setClassName(context, SERVICE_CLASS_NAME)
            action = ACTION_START
            putExtra(EXTRA_PROMPT_ID, promptId)
            putExtra(EXTRA_BASE_URL, baseUrl)
            putExtra(EXTRA_WS_SCHEME, wsScheme)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Failed to start monitor service: ${e.message}")
        }
    }

    actual fun stopMonitoring() {
        val intent = Intent().apply {
            setClassName(context, SERVICE_CLASS_NAME)
            action = ACTION_STOP
        }
        try {
            context.startService(intent)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Failed to stop monitor service: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "BackgroundMonitorStarter"
        private const val SERVICE_CLASS_NAME =
            "com.riox432.civitdeck.service.GenerationMonitorService"
        const val ACTION_START = "com.riox432.civitdeck.action.START_MONITOR"
        const val ACTION_STOP = "com.riox432.civitdeck.action.STOP_MONITOR"
        const val EXTRA_PROMPT_ID = "extra_prompt_id"
        const val EXTRA_BASE_URL = "extra_base_url"
        const val EXTRA_WS_SCHEME = "extra_ws_scheme"
    }
}
