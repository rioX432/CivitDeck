package com.riox432.civitdeck.download

import com.riox432.civitdeck.domain.download.DownloadScheduler
import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository
import com.riox432.civitdeck.domain.util.ApplicationScope
import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest

/**
 * JVM/Desktop [DownloadScheduler] backed by a Ktor streaming download.
 *
 * Desktop has no OS-level download manager (no WorkManager as on Android, no background
 * `URLSession` as on iOS), so [enqueue] launches a coroutine on the app-lifetime
 * [ApplicationScope] instead — it keeps running while the user navigates away from the
 * screen that started it, and is cancelled from [cancel].
 *
 * Like Android's `ModelDownloadWorker`, there is no true byte-range resume: a re-[enqueue]
 * (triggered by the queue's "resume"/"retry" actions) restarts the file from byte 0.
 */
class DesktopDownloadScheduler(
    private val repository: ModelDownloadRepository,
    private val httpClient: HttpClient,
    private val scope: ApplicationScope,
    private val downloadRoot: () -> File = { defaultDownloadRoot() },
) : DownloadScheduler {

    private val jobsLock = Mutex()
    private val activeJobs = mutableMapOf<Long, Job>()

    override fun enqueue(downloadId: Long) {
        scope.launch {
            jobsLock.withLock {
                if (activeJobs.containsKey(downloadId)) return@withLock
                activeJobs[downloadId] = scope.launch { runDownload(downloadId) }
            }
        }
    }

    override fun cancel(downloadId: Long) {
        scope.launch {
            val job = jobsLock.withLock { activeJobs.remove(downloadId) }
            job?.cancel()
        }
    }

    private suspend fun runDownload(downloadId: Long) {
        try {
            val download = repository.getDownloadById(downloadId) ?: return
            repository.updateStatus(downloadId, DownloadStatus.Downloading)
            executeDownload(downloadId, download)
        } catch (e: CancellationException) {
            repository.updateStatus(downloadId, DownloadStatus.Cancelled)
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "Download failed for id $downloadId: ${e.message}", e)
            repository.updateStatus(downloadId, DownloadStatus.Failed, e.message)
        } finally {
            jobsLock.withLock { activeJobs.remove(downloadId) }
        }
    }

    private suspend fun executeDownload(downloadId: Long, download: ModelDownload) {
        val destDir = File(downloadRoot(), download.modelType)
        destDir.mkdirs()
        val destFile = File(destDir, download.fileName)

        httpClient.prepareGet(download.fileUrl).execute { response ->
            if (!response.status.isSuccess()) {
                repository.updateStatus(downloadId, DownloadStatus.Failed, "HTTP ${response.status.value}")
                return@execute
            }
            streamToFile(downloadId, response, destFile)
            repository.updateDestinationPath(downloadId, destFile.absolutePath)
            verifyHash(downloadId, download.expectedSha256, destFile)
            repository.updateStatus(downloadId, DownloadStatus.Completed)
        }
    }

    private suspend fun streamToFile(downloadId: Long, response: HttpResponse, destFile: File) {
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
        var downloadedBytes = 0L
        destFile.outputStream().use { output ->
            while (true) {
                val read = channel.readAvailable(buffer)
                if (read == -1) break
                if (read <= 0) continue
                output.write(buffer, 0, read)
                downloadedBytes += read
                repository.updateProgress(downloadId, downloadedBytes)
            }
        }
    }

    private suspend fun verifyHash(downloadId: Long, expectedSha256: String?, file: File) {
        if (expectedSha256.isNullOrEmpty()) return
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(HASH_BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        @OptIn(ExperimentalStdlibApi::class)
        val actualHash = digest.digest().toHexString()
        repository.updateHashVerified(downloadId, actualHash.equals(expectedSha256, ignoreCase = true))
    }

    companion object {
        private const val TAG = "DesktopDownloadScheduler"
        private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
        private const val HASH_BUFFER_SIZE = 8192
    }
}

private fun defaultDownloadRoot(): File = File(File(System.getProperty("user.home"), ".civitdeck"), "Downloads")
