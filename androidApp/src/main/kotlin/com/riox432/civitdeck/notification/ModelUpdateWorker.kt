package com.riox432.civitdeck.notification

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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ModelUpdate
import com.riox432.civitdeck.domain.usecase.CheckAndStoreModelUpdatesUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.cancellation.CancellationException

class ModelUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val checkAndStoreUseCase: CheckAndStoreModelUpdatesUseCase by inject()

    override suspend fun doWork(): Result {
        return try {
            val updates = checkAndStoreUseCase()
            if (updates.isNotEmpty()) {
                showNotification(updates)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(updates: List<ModelUpdate>) {
        if (!hasNotificationPermission()) return
        ensureNotificationChannel()

        val title = if (updates.size == 1) {
            "${updates.first().modelName} updated"
        } else {
            "${updates.size} models updated"
        }

        val body = updates.joinToString("\n") { "${it.modelName}: ${it.newVersionName}" }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(if (updates.size == 1) "New version: ${updates.first().newVersionName}" else body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        createNotificationChannel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Model Updates",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifications when favorited models get new versions"
        }
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val WORK_NAME = "model_update_check"
        const val CHANNEL_ID = "model_updates"
        const val NOTIFICATION_ID = 1001
    }
}
