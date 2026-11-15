package com.riox432.civitdeck.download

import com.riox432.civitdeck.domain.download.DownloadScheduler
import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository
import com.riox432.civitdeck.domain.usecase.VerifyDownloadHashUseCase
import com.riox432.civitdeck.domain.util.ApplicationScope
import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
    private val verifyDownloadHash: VerifyDownloadHashUseCase,
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
        // Streamed beside the destination so an existing good file survives until the
        // replacement passes verification, and the final move stays on one filesystem. Unique
        // per transfer: a cancelled job's cleanup can still run after a resume has started.
        val partFile = File.createTempFile("${download.fileName}.", ".part", destDir)

        try {
            httpClient.prepareGet(download.fileUrl).execute { response ->
                if (!response.status.isSuccess()) {
                    repository.updateStatus(downloadId, DownloadStatus.Failed, "HTTP ${response.status.value}")
                    return@execute
                }
                streamToFile(downloadId, response, partFile)
                repository.updateDestinationPath(downloadId, destFile.absolutePath)
                if (verifyDownloadHash(downloadId, computeSha256(download.expectedSha256, partFile))) {
                    replaceDestination(partFile, destFile)
                }
            }
        } finally {
            partFile.delete()
        }
    }

    private suspend fun streamToFile(downloadId: Long, response: HttpResponse, destFile: File) {
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
        var downloadedBytes = 0L
        destFile.outputStream().use { output ->
            var read: Int
            while (channel.readAvailable(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                downloadedBytes += read
                repository.updateProgress(downloadId, downloadedBytes)
            }
        }
    }

    /**
     * A non-atomic REPLACE_EXISTING move deletes the target before moving the source, so it is
     * only the fallback for filesystems that cannot rename atomically.
     */
    private fun replaceDestination(source: File, destination: File) {
        try {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /**
     * Returns the file's SHA-256 as hex, or null when CivitAI published no hash or the file
     * could not be read; [VerifyDownloadHashUseCase] treats null as unverified, not a mismatch.
     */
    private fun computeSha256(expectedSha256: String?, file: File): String? {
        if (expectedSha256.isNullOrBlank()) return null
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(HASH_BUFFER_SIZE)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            @OptIn(ExperimentalStdlibApi::class)
            digest.digest().toHexString()
        } catch (e: IOException) {
            Logger.w(TAG, "Hash computation failed for ${file.name}: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "DesktopDownloadScheduler"
        private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
        private const val HASH_BUFFER_SIZE = 8192
    }
}

private fun defaultDownloadRoot(): File = File(File(System.getProperty("user.home"), ".civitdeck"), "Downloads")
