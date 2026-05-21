package com.riox432.civitdeck.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.riox432.civitdeck.R
import com.riox432.civitdeck.data.api.comfyui.ComfyUIWebSocketApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIWebSocketMessage
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarter.Companion.ACTION_START
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarter.Companion.ACTION_STOP
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarter.Companion.EXTRA_BASE_URL
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarter.Companion.EXTRA_PROMPT_ID
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarter.Companion.EXTRA_WS_SCHEME
import com.riox432.civitdeck.domain.service.GenerationNotificationService
import com.riox432.civitdeck.domain.util.currentTimeMillis
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Android foreground service that maintains a WebSocket connection to a ComfyUI server
 * in the background. Shows a persistent notification with generation progress and
 * fires a completion/error notification when the job finishes.
 *
 * Lifecycle:
 * - Started by [BackgroundMonitorStarter] after generation is submitted
 * - Stops itself when [ComfyUIWebSocketMessage.ExecutionSuccess] or
 *   [ComfyUIWebSocketMessage.ExecutionError] is received
 * - Can be stopped externally via [ACTION_STOP] intent
 */
class GenerationMonitorService : Service() {

    private val webSocketApi: ComfyUIWebSocketApi by inject()
    private val notificationService: GenerationNotificationService by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null
    private var startTimeMs: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_STOP -> stopSelfCleanly()
            else -> stopSelfCleanly()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleStart(intent: Intent) {
        val promptId = intent.getStringExtra(EXTRA_PROMPT_ID) ?: return stopSelfCleanly()
        val baseUrl = intent.getStringExtra(EXTRA_BASE_URL) ?: return stopSelfCleanly()
        val wsScheme = intent.getStringExtra(EXTRA_WS_SCHEME) ?: "ws"

        startTimeMs = currentTimeMillis()

        // Cancel any existing monitor before starting a new one
        monitorJob?.cancel()

        startForegroundNotification()
        monitorJob = serviceScope.launch {
            collectWebSocket(promptId, baseUrl, wsScheme)
        }
    }

    private fun startForegroundNotification() {
        val notification = buildProgressNotification(
            getString(R.string.generation_monitor_waiting),
            progress = 0,
            maxProgress = 0,
            indeterminate = true,
        )
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification.build(),
            foregroundServiceType(),
        )
    }

    private fun foregroundServiceType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
    }

    private suspend fun collectWebSocket(
        promptId: String,
        baseUrl: String,
        wsScheme: String,
    ) {
        val clientId = "civitdeck-monitor-${currentTimeMillis()}"
        try {
            webSocketApi.observeProgress(baseUrl, wsScheme, clientId, promptId)
                .collect { message -> handleMessage(message, promptId) }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "WebSocket monitor failed: ${e.message}")
            notificationService.notifyGenerationError(promptId, e.message ?: "Connection lost")
        }
        stopSelfCleanly()
    }

    private fun handleMessage(message: ComfyUIWebSocketMessage, promptId: String) {
        when (message) {
            is ComfyUIWebSocketMessage.Progress -> updateProgress(message)
            is ComfyUIWebSocketMessage.ExecutionSuccess -> onComplete(promptId)
            is ComfyUIWebSocketMessage.ExecutionError -> onError(promptId, message)
            else -> { /* Status, Executing, etc. — no notification update needed */ }
        }
    }

    private fun updateProgress(progress: ComfyUIWebSocketMessage.Progress) {
        val contentText = getString(
            R.string.generation_monitor_progress,
            progress.value,
            progress.max,
        )
        val notification = buildProgressNotification(
            contentText,
            progress = progress.value,
            maxProgress = progress.max,
            indeterminate = false,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification.build())
    }

    private fun onComplete(promptId: String) {
        val elapsed = currentTimeMillis() - startTimeMs
        notificationService.notifyGenerationComplete(promptId, imageCount = 1, elapsed)
    }

    private fun onError(promptId: String, error: ComfyUIWebSocketMessage.ExecutionError) {
        notificationService.notifyGenerationError(promptId, error.exceptionMessage)
    }

    private fun buildProgressNotification(
        contentText: String,
        progress: Int,
        maxProgress: Int,
        indeterminate: Boolean,
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.generation_monitor_title))
            .setContentText(contentText)
            .setProgress(maxProgress, progress, indeterminate)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    }

    private fun stopSelfCleanly() {
        monitorJob?.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        createChannel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.generation_monitor_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.generation_monitor_channel_description)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "GenerationMonitorSvc"
        private const val CHANNEL_ID = "generation_monitor"
        private const val NOTIFICATION_ID = 3001
    }
}
