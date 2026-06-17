package com.riox432.civitdeck.domain.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class GenerationNotificationServiceImpl(
    private val context: Context,
) : GenerationNotificationService {

    init {
        ensureChannel()
    }

    override fun notifyGenerationComplete(promptId: String, imageCount: Int, elapsedMs: Long) {
        if (!hasNotificationPermission()) return

        val elapsedSec = elapsedMs / MILLIS_PER_SECOND
        val title = "Generation Complete"
        val body = buildString {
            append("$imageCount image")
            if (imageCount != 1) append("s")
            append(" generated in ${elapsedSec}s")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(promptId.hashCode(), notification)
    }

    override fun notifyGenerationError(promptId: String, errorMessage: String) {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Generation Failed")
            .setContentText(errorMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(promptId.hashCode(), notification)
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        createChannel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications when ComfyUI generation completes or fails"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private companion object {
        private const val CHANNEL_ID = "generation_complete"
        private const val CHANNEL_NAME = "Generation Complete"
        private const val MILLIS_PER_SECOND = 1000L
    }
}
