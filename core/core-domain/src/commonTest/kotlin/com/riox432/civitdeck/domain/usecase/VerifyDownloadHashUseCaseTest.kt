package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VerifyDownloadHashUseCaseTest {

    @Test
    fun missing_download_writes_nothing_and_returns_false() = runTest {
        val repo = RecordingDownloadRepo(download = null)

        val result = VerifyDownloadHashUseCase(repo)(downloadId = ID, actualSha256 = "abc")

        assertFalse(result)
        assertEquals(emptyList<Write>(), repo.writes)
    }

    @Test
    fun download_without_expected_hash_completes_without_touching_hash_verified() = runTest {
        val repo = RecordingDownloadRepo(download(expectedSha256 = null))

        val result = VerifyDownloadHashUseCase(repo)(downloadId = ID, actualSha256 = "abc")

        assertTrue(result)
        assertEquals(listOf<Write>(Write.Status(ID, DownloadStatus.Completed, null)), repo.writes)
    }

    @Test
    fun download_with_blank_expected_hash_completes_without_touching_hash_verified() = runTest {
        val repo = RecordingDownloadRepo(download(expectedSha256 = "  "))

        val result = VerifyDownloadHashUseCase(repo)(downloadId = ID, actualSha256 = "abc")

        assertTrue(result)
        assertEquals(listOf<Write>(Write.Status(ID, DownloadStatus.Completed, null)), repo.writes)
    }

    @Test
    fun failed_hash_computation_completes_without_touching_hash_verified() = runTest {
        val repo = RecordingDownloadRepo(download(expectedSha256 = "ABC123"))

        val result = VerifyDownloadHashUseCase(repo)(downloadId = ID, actualSha256 = null)

        assertTrue(result)
        assertEquals(listOf<Write>(Write.Status(ID, DownloadStatus.Completed, null)), repo.writes)
    }

    @Test
    fun matching_hash_ignoring_case_marks_verified_and_completed() = runTest {
        val repo = RecordingDownloadRepo(download(expectedSha256 = "ABC123"))

        val result = VerifyDownloadHashUseCase(repo)(downloadId = ID, actualSha256 = "abc123")

        assertTrue(result)
        assertEquals(
            listOf(
                Write.HashVerified(ID, verified = true),
                Write.Status(ID, DownloadStatus.Completed, null),
            ),
            repo.writes,
        )
    }

    @Test
    fun mismatched_hash_marks_unverified_and_failed_without_error_message() = runTest {
        val repo = RecordingDownloadRepo(download(expectedSha256 = "ABC123"))

        val result = VerifyDownloadHashUseCase(repo)(downloadId = ID, actualSha256 = "def456")

        assertFalse(result)
        assertEquals(
            listOf(
                Write.HashVerified(ID, verified = false),
                Write.Status(ID, DownloadStatus.Failed, null),
            ),
            repo.writes,
        )
    }

    private fun download(expectedSha256: String?) = ModelDownload(
        id = ID,
        modelId = 10L,
        modelName = "M",
        versionId = 20L,
        versionName = "v",
        fileId = 30L,
        fileName = "f.safetensors",
        fileUrl = "url",
        fileSizeBytes = 100L,
        status = DownloadStatus.Downloading,
        modelType = "Checkpoint",
        destinationPath = "/models/f.safetensors",
        expectedSha256 = expectedSha256,
    )

    private sealed interface Write {
        data class Status(val id: Long, val status: DownloadStatus, val errorMessage: String?) : Write
        data class HashVerified(val id: Long, val verified: Boolean) : Write
    }

    private class RecordingDownloadRepo(private val download: ModelDownload?) : ModelDownloadRepository {
        val writes = mutableListOf<Write>()

        override suspend fun getDownloadById(id: Long): ModelDownload? = download?.takeIf { it.id == id }
        override suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String?) {
            writes += Write.Status(id, status, errorMessage)
        }
        override suspend fun updateHashVerified(id: Long, verified: Boolean) {
            writes += Write.HashVerified(id, verified)
        }
        override suspend fun enqueueDownload(download: ModelDownload): Long = throw NotImplementedError()
        override fun observeAllDownloads(): Flow<List<ModelDownload>> = throw NotImplementedError()
        override fun observeDownloadsForModel(modelId: Long): Flow<List<ModelDownload>> =
            throw NotImplementedError()
        override suspend fun getDownloadByFileId(fileId: Long): ModelDownload? =
            throw NotImplementedError()
        override suspend fun updateProgress(id: Long, downloadedBytes: Long) =
            throw NotImplementedError()
        override suspend fun updateDestinationPath(id: Long, path: String) = throw NotImplementedError()
        override suspend fun deleteDownload(id: Long) = throw NotImplementedError()
        override suspend fun clearCompletedDownloads() = throw NotImplementedError()
    }

    private companion object {
        const val ID = 1L
    }
}
