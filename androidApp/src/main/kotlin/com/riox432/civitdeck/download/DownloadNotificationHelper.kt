package com.riox432.civitdeck.download

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
import com.riox432.civitdeck.R

object DownloadNotificationHelper {

    const val CHANNEL_ID = "model_downloads"
    private const val PROGRESS_NOTIFICATION_ID_BASE = 2000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        createChannel(context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Model Downloads",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Download progress for model files"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun progressNotification(
        context: Context,
        fileName: String,
        progress: Int,
    ): NotificationCompat.Builder {
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Downloading $fileName")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setSilent(true)
    }

    fun showCompleted(context: Context, downloadId: Long, fileName: String) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Download complete")
            .setContentText(fileName)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context)
            .notify(notificationId(downloadId), notification)
    }

    fun notificationId(downloadId: Long): Int =
        PROGRESS_NOTIFICATION_ID_BASE + downloadId.toInt()

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
