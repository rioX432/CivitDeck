package com.riox432.civitdeck.download

import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository
import com.riox432.civitdeck.domain.usecase.VerifyDownloadHashUseCase
import com.riox432.civitdeck.domain.util.ApplicationScope
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.io.File
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    /**
     * Mock client whose engine runs on the test scheduler. The engine default is a real IO
     * dispatcher, so `advanceUntilIdle()` would return before the transfer finishes and every
     * assertion below would see status `Downloading` instead of the final state.
     */
    private fun TestScope.client(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient = HttpClient(
        MockEngine.create {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler(handler)
        },
    )

    private fun TestScope.okClient(): HttpClient =
        client { respond(content = ByteReadChannel(FILE_BYTES), status = HttpStatusCode.OK) }

    private fun TestScope.scheduler(
        repository: FakeModelDownloadRepository,
        httpClient: HttpClient,
        downloadRoot: File,
    ) = DesktopDownloadScheduler(
        repository = repository,
        httpClient = httpClient,
        scope = ApplicationScope(this),
        verifyDownloadHash = VerifyDownloadHashUseCase(repository),
        downloadRoot = { downloadRoot },
    )

    @Test
    fun enqueue_downloads_file_and_marks_completed() = runTest {
        val repository = FakeModelDownloadRepository(download())
        val tempDir = createTempDirectory()

        scheduler(repository, okClient(), tempDir).enqueue(1L)
        advanceUntilIdle()

        assertEquals(DownloadStatus.Completed, repository.lastStatus)
        assertEquals(FILE_BYTES.size.toLong(), repository.lastProgress)
        assertContentEquals(FILE_BYTES, destFile(tempDir).readBytes())
        assertNoPartFiles(tempDir)
    }

    @Test
    fun enqueue_marks_failed_with_http_status_on_error_response() = runTest {
        val repository = FakeModelDownloadRepository(download())
        val tempDir = createTempDirectory()
        val httpClient = client {
            respond(
                content = ByteReadChannel(ByteArray(0)),
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(),
            )
        }

        scheduler(repository, httpClient, tempDir).enqueue(1L)
        advanceUntilIdle()

        assertEquals(DownloadStatus.Failed, repository.lastStatus)
        assertEquals("HTTP 401", repository.lastErrorMessage)
        assertNull(repository.destinationPath)
        assertFalse(destFile(tempDir).exists())
    }

    @Test
    fun enqueue_completes_verified_when_hash_matches() = runTest {
        val repository = FakeModelDownloadRepository(download(expectedSha256 = FILE_SHA256.uppercase()))
        val tempDir = createTempDirectory()

        scheduler(repository, okClient(), tempDir).enqueue(1L)
        advanceUntilIdle()

        assertEquals(DownloadStatus.Completed, repository.lastStatus)
        assertEquals(true, repository.hashVerified)
        assertEquals(destFile(tempDir).absolutePath, repository.destinationPath)
        assertContentEquals(FILE_BYTES, destFile(tempDir).readBytes())
        assertNoPartFiles(tempDir)
    }

    @Test
    fun enqueue_marks_failed_and_deletes_file_when_hash_does_not_match() = runTest {
        val repository = FakeModelDownloadRepository(download(expectedSha256 = "deadbeef"))
        val tempDir = createTempDirectory()

        scheduler(repository, okClient(), tempDir).enqueue(1L)
        advanceUntilIdle()

        assertEquals(false, repository.hashVerified)
        assertEquals(DownloadStatus.Failed, repository.lastStatus)
        assertFalse(destFile(tempDir).exists())
        assertNoPartFiles(tempDir)
    }

    @Test
    fun enqueue_keeps_existing_destination_when_hash_does_not_match() = runTest {
        val repository = FakeModelDownloadRepository(download(expectedSha256 = "deadbeef"))
        val tempDir = createTempDirectory()
        val existing = destFile(tempDir).apply {
            parentFile.mkdirs()
            writeBytes(EXISTING_BYTES)
        }

        scheduler(repository, okClient(), tempDir).enqueue(1L)
        advanceUntilIdle()

        assertEquals(DownloadStatus.Failed, repository.lastStatus)
        assertContentEquals(EXISTING_BYTES, existing.readBytes())
        assertNoPartFiles(tempDir)
    }

    @Test
    fun enqueue_replaces_existing_destination_when_hash_matches() = runTest {
        val repository = FakeModelDownloadRepository(download(expectedSha256 = FILE_SHA256))
        val tempDir = createTempDirectory()
        val existing = destFile(tempDir).apply {
            parentFile.mkdirs()
            writeBytes(EXISTING_BYTES)
        }

        scheduler(repository, okClient(), tempDir).enqueue(1L)
        advanceUntilIdle()

        assertEquals(DownloadStatus.Completed, repository.lastStatus)
        assertContentEquals(FILE_BYTES, existing.readBytes())
    }

    @Test
    fun enqueue_completes_unverified_when_no_hash_is_published() = runTest {
        val repository = FakeModelDownloadRepository(download(expectedSha256 = null))
        val tempDir = createTempDirectory()

        scheduler(repository, okClient(), tempDir).enqueue(1L)
        advanceUntilIdle()

        assertEquals(DownloadStatus.Completed, repository.lastStatus)
        assertNull(repository.hashVerified)
        assertContentEquals(FILE_BYTES, destFile(tempDir).readBytes())
    }

    @Test
    fun enqueue_is_idempotent_while_a_download_is_already_active() = runTest {
        val repository = FakeModelDownloadRepository(download())
        val tempDir = createTempDirectory()
        var requestCount = 0
        val httpClient = client {
            requestCount++
            respond(content = ByteReadChannel(FILE_BYTES), status = HttpStatusCode.OK)
        }
        val scheduler = scheduler(repository, httpClient, tempDir)

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
        val scheduler = scheduler(repository, client { respond("") }, createTempDirectory())

        scheduler.cancel(999L)
        advanceUntilIdle()

        assertNull(repository.lastStatus)
    }

    private fun destFile(root: File) = File(File(root, "Checkpoint"), "model.safetensors")

    private fun assertNoPartFiles(root: File) {
        val leftovers = root.walk().filter { it.isFile && it.name.endsWith(".part") }.toList()
        assertTrue(leftovers.isEmpty(), "Unexpected partial files: $leftovers")
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
        private val EXISTING_BYTES = "known-good-model-contents".encodeToByteArray()

        @OptIn(ExperimentalStdlibApi::class)
        private val FILE_SHA256 = MessageDigest.getInstance("SHA-256").digest(FILE_BYTES).toHexString()
    }
}
