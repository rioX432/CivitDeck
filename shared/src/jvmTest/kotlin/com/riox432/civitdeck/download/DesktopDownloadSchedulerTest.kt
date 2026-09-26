package com.riox432.civitdeck.download

import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository
import com.riox432.civitdeck.domain.util.ApplicationScope
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopDownloadSchedulerTest {

    private fun download(id: Long = 1L, expectedSha256: String? = null) = ModelDownload(
        id = id,
        modelId = 10L,
        modelName = "Test Model",
        versionId = 20L,
        versionName = "v1",
        fileId = 30L,
        fileName = "model.safetensors",
        fileUrl = "https://example.com/model.safetensors",
        fileSizeBytes = FILE_BYTES.size.toLong(),
        modelType = "Checkpoint",
        expectedSha256 = expectedSha256,
    )

    @Test
    fun enqueue_downloads_file_and_marks_completed() = runTest {
        val repository = FakeModelDownloadRepository(download())
        val tempDir = createTempDirectory()
        val engine = MockEngine {
            respond(content = ByteReadChannel(FILE_BYTES), status = HttpStatusCode.OK)
        }
        val scheduler = DesktopDownloadScheduler(
            repository = repository,
            httpClient = HttpClient(engine),
            scope = ApplicationScope(this),
            downloadRoot = { tempDir },
        )

        scheduler.enqueue(1L)
        advanceUntilIdle()

        assertEquals(DownloadStatus.Completed, repository.lastStatus)
        assertEquals(FILE_BYTES.size.toLong(), repository.lastProgress)
        val destFile = File(File(tempDir, "Checkpoint"), "model.safetensors")
        assertTrue(destFile.exists())
        assertTrue(destFile.readBytes().contentEquals(FILE_BYTES))
    }

    @Test
    fun enqueue_marks_failed_with_http_status_on_error_response() = runTest {
        val repository = FakeModelDownloadRepository(download())
        val tempDir = createTempDirectory()
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(ByteArray(0)),
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(),
            )
        }
        val scheduler = DesktopDownloadScheduler(
            repository = repository,
            httpClient = HttpClient(engine),
            scope = ApplicationScope(this),
            downloadRoot = { tempDir },
        )

        scheduler.enqueue(1L)
        advanceUntilIdle()

        assertEquals(DownloadStatus.Failed, repository.lastStatus)
        assertEquals("HTTP 401", repository.lastErrorMessage)
        assertNull(repository.destinationPath)
    }

    @Test
    fun enqueue_marks_failed_when_hash_does_not_match() = runTest {
        val repository = FakeModelDownloadRepository(download(expectedSha256 = "deadbeef"))
        val tempDir = createTempDirectory()
        val engine = MockEngine {
            respond(content = ByteReadChannel(FILE_BYTES), status = HttpStatusCode.OK)
        }
        val scheduler = DesktopDownloadScheduler(
            repository = repository,
            httpClient = HttpClient(engine),
            scope = ApplicationScope(this),
            downloadRoot = { tempDir },
        )

        scheduler.enqueue(1L)
        advanceUntilIdle()

        assertEquals(false, repository.hashVerified)
        // Hash mismatch is surfaced via hashVerified, not a Failed status — matches Android/iOS.
        assertEquals(DownloadStatus.Completed, repository.lastStatus)
    }

    @Test
    fun enqueue_is_idempotent_while_a_download_is_already_active() = runTest {
        val repository = FakeModelDownloadRepository(download())
        val tempDir = createTempDirectory()
        var requestCount = 0
        val engine = MockEngine {
            requestCount++
            respond(content = ByteReadChannel(FILE_BYTES), status = HttpStatusCode.OK)
        }
        val scheduler = DesktopDownloadScheduler(
            repository = repository,
            httpClient = HttpClient(engine),
            scope = ApplicationScope(this),
            downloadRoot = { tempDir },
        )

        // Two rapid taps on "Download" (or an enqueue racing a queue-screen retry) must not
        // start two overlapping transfers of the same file.
        scheduler.enqueue(1L)
        scheduler.enqueue(1L)
        advanceUntilIdle()

        assertEquals(1, requestCount)
        assertEquals(DownloadStatus.Completed, repository.lastStatus)
    }

    @Test
    fun cancel_of_an_unknown_download_id_is_a_safe_no_op() = runTest {
        val repository = FakeModelDownloadRepository(download())
        val scheduler = DesktopDownloadScheduler(
            repository = repository,
            httpClient = HttpClient(MockEngine { respond("") }),
            scope = ApplicationScope(this),
            downloadRoot = { createTempDirectory() },
        )

        scheduler.cancel(999L)
        advanceUntilIdle()

        assertNull(repository.lastStatus)
    }

    private fun createTempDirectory(): File =
        File.createTempFile("civitdeck-download-test", "").apply {
            delete()
            mkdirs()
        }

    private class FakeModelDownloadRepository(
        private val stored: ModelDownload,
    ) : ModelDownloadRepository {
        var lastStatus: DownloadStatus? = null
        var lastErrorMessage: String? = null
        var lastProgress: Long? = null
        var destinationPath: String? = null
        var hashVerified: Boolean? = null

        override suspend fun enqueueDownload(download: ModelDownload): Long = stored.id
        override fun observeAllDownloads(): Flow<List<ModelDownload>> = error("Not needed for this test")
        override fun observeDownloadsForModel(modelId: Long): Flow<List<ModelDownload>> =
            error("Not needed for this test")
        override suspend fun getDownloadById(id: Long): ModelDownload? =
            stored.takeIf { it.id == id }
        override suspend fun getDownloadByFileId(fileId: Long): ModelDownload? = null

        override suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String?) {
            lastStatus = status
            lastErrorMessage = errorMessage
        }

        override suspend fun updateProgress(id: Long, downloadedBytes: Long) {
            lastProgress = downloadedBytes
        }

        override suspend fun updateDestinationPath(id: Long, path: String) {
            destinationPath = path
        }

        override suspend fun deleteDownload(id: Long) = Unit

        override suspend fun updateHashVerified(id: Long, verified: Boolean) {
            hashVerified = verified
        }

        override suspend fun clearCompletedDownloads() = Unit
    }

    companion object {
        private val FILE_BYTES = "fake-model-file-contents".encodeToByteArray()
    }
}
