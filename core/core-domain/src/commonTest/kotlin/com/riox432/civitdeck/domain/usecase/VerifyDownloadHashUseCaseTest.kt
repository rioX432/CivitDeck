package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class VerifyDownloadHashUseCaseTest {

    @Test
    fun returns_not_found_when_download_missing() = runTest {
        val useCase = VerifyDownloadHashUseCase(FakeDownloadRepo(download = null))

        val result = useCase(downloadId = 1L, expectedSha256 = "abc")

        assertEquals(HashVerificationResult.NotFound, result)
    }

    @Test
    fun returns_no_file_when_destination_path_is_null() = runTest {
        val download = download(id = 1L, destinationPath = null)
        val useCase = VerifyDownloadHashUseCase(FakeDownloadRepo(download))

        val result = useCase(downloadId = 1L, expectedSha256 = "abc")

        assertEquals(HashVerificationResult.NoFile, result)
    }

    @Test
    fun returns_pending_with_path_and_expected_hash() = runTest {
        val download = download(id = 1L, destinationPath = "/models/file.safetensors")
        val useCase = VerifyDownloadHashUseCase(FakeDownloadRepo(download))

        val result = useCase(downloadId = 1L, expectedSha256 = "EXPECTED")

        val pending = assertIs<HashVerificationResult.Pending>(result)
        assertEquals("/models/file.safetensors", pending.filePath)
        assertEquals("EXPECTED", pending.expectedHash)
    }

    private fun download(id: Long, destinationPath: String?) = ModelDownload(
        id = id,
        modelId = 10L,
        modelName = "M",
        versionId = 20L,
        versionName = "v",
        fileId = 30L,
        fileName = "f.safetensors",
        fileUrl = "url",
        fileSizeBytes = 100L,
        modelType = "Checkpoint",
        destinationPath = destinationPath,
    )

    private class FakeDownloadRepo(private val download: ModelDownload?) : ModelDownloadRepository {
        override suspend fun enqueueDownload(download: ModelDownload): Long = throw NotImplementedError()
        override fun observeAllDownloads(): Flow<List<ModelDownload>> = throw NotImplementedError()
        override fun observeDownloadsForModel(modelId: Long): Flow<List<ModelDownload>> =
            throw NotImplementedError()
        override suspend fun getDownloadById(id: Long): ModelDownload? = download
        override suspend fun getDownloadByFileId(fileId: Long): ModelDownload? =
            throw NotImplementedError()
        override suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String?) =
            throw NotImplementedError()
        override suspend fun updateProgress(id: Long, downloadedBytes: Long) =
            throw NotImplementedError()
        override suspend fun updateDestinationPath(id: Long, path: String) = throw NotImplementedError()
        override suspend fun deleteDownload(id: Long) = throw NotImplementedError()
        override suspend fun updateHashVerified(id: Long, verified: Boolean) =
            throw NotImplementedError()
        override suspend fun clearCompletedDownloads() = throw NotImplementedError()
    }
}
