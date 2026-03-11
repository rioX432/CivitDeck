package com.riox432.civitdeck.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class ModelDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val repository: ModelDownloadRepository by inject()
    private val apiKeyProvider: ApiKeyProvider by inject()

    @Suppress("LongMethod")
    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1)
        if (downloadId == -1L) return Result.failure()

        val download = repository.getDownloadById(downloadId) ?: return Result.failure()

        repository.updateStatus(downloadId, DownloadStatus.Downloading)

        val foreground = createForegroundInfo(downloadId, download.fileName, 0)
        setForeground(foreground)

        return try {
            executeDownload(downloadId, download)
        } catch (e: CancellationException) {
            repository.updateStatus(downloadId, DownloadStatus.Cancelled)
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            repository.updateStatus(downloadId, DownloadStatus.Failed, e.message)
            Result.failure()
        }
    }

    private suspend fun executeDownload(
        downloadId: Long,
        download: com.riox432.civitdeck.domain.model.ModelDownload,
    ): Result {
        val destDir = File(applicationContext.getExternalFilesDir(null), download.modelType)
        destDir.mkdirs()
        val destFile = File(destDir, download.fileName)

        val requestBuilder = Request.Builder().url(download.fileUrl)
        apiKeyProvider.apiKey?.let { key ->
            requestBuilder.addHeader("Authorization", "Bearer $key")
        }

        val response = OkHttpClient().newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            repository.updateStatus(downloadId, DownloadStatus.Failed, "HTTP ${response.code}")
            return Result.failure()
        }
        val body = response.body ?: run {
            repository.updateStatus(downloadId, DownloadStatus.Failed, "Empty response")
            return Result.failure()
        }

        val bytes = streamToFile(downloadId, download.fileName, body, destFile)
        if (bytes < 0) return Result.failure()

        repository.updateProgress(downloadId, bytes)
        repository.updateDestinationPath(downloadId, destFile.absolutePath)
        repository.updateStatus(downloadId, DownloadStatus.Completed)
        DownloadNotificationHelper.showCompleted(applicationContext, downloadId, download.fileName)
        return Result.success()
    }

    private suspend fun streamToFile(
        downloadId: Long,
        fileName: String,
        body: okhttp3.ResponseBody,
        destFile: File,
    ): Long {
        val totalBytes = body.contentLength()
        var downloadedBytes = 0L
        var lastProgress = 0
        val input = body.byteStream()
        val output = destFile.outputStream()
        try {
            val buffer = ByteArray(BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                if (isStopped) {
                    repository.updateStatus(downloadId, DownloadStatus.Cancelled)
                    return -1
                }
                output.write(buffer, 0, read)
                downloadedBytes += read
                val progress = if (totalBytes > 0) {
                    ((downloadedBytes * PERCENT_MAX) / totalBytes).toInt()
                } else {
                    0
                }
                if (progress != lastProgress) {
                    lastProgress = progress
                    repository.updateProgress(downloadId, downloadedBytes)
                    setForeground(createForegroundInfo(downloadId, fileName, progress))
                }
            }
        } finally {
            input.close()
            output.close()
        }
        return downloadedBytes
    }

    private fun createForegroundInfo(
        downloadId: Long,
        fileName: String,
        progress: Int,
    ): ForegroundInfo {
        val notification = DownloadNotificationHelper.progressNotification(
            applicationContext,
            fileName,
            progress,
        ).build()
        val notificationId = DownloadNotificationHelper.notificationId(downloadId)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        private const val BUFFER_SIZE = 8192
        private const val PERCENT_MAX = 100
    }
}
