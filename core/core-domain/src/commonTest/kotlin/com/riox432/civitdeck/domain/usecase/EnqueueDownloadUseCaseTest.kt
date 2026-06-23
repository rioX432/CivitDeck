package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers [EnqueueDownloadUseCase]: enqueueing is idempotent per fileId — an
 * already-queued file returns the existing row id instead of inserting a duplicate.
 */
class EnqueueDownloadUseCaseTest {

    @Test
    fun enqueues_and_returns_new_id_when_file_is_not_already_queued() = runTest {
        val repo = FakeDownloadRepo(existingByFileId = emptyMap(), newId = 42L)
        val useCase = EnqueueDownloadUseCase(repo)

        val id = useCase(download(fileId = 7L))

        assertEquals(42L, id)
        assertTrue(repo.enqueueCalled)
    }

    @Test
    fun returns_existing_id_without_re_enqueueing_when_file_already_queued() = runTest {
        val existing = download(id = 99L, fileId = 7L)
        val repo = FakeDownloadRepo(existingByFileId = mapOf(7L to existing), newId = 42L)
        val useCase = EnqueueDownloadUseCase(repo)

        val id = useCase(download(fileId = 7L))

        // Idempotent: the existing row id is returned and no new insert happens.
        assertEquals(99L, id)
        assertFalse(repo.enqueueCalled)
    }

    private fun download(id: Long = 0L, fileId: Long) = ModelDownload(
        id = id,
        modelId = 1L,
        modelName = "Model",
        versionId = 1L,
        versionName = "v1",
        fileId = fileId,
        fileName = "model.safetensors",
        fileUrl = "https://example.com/model.safetensors",
        fileSizeBytes = 1000L,
        modelType = "Checkpoint",
    )

    private class FakeDownloadRepo(
        private val existingByFileId: Map<Long, ModelDownload>,
        private val newId: Long,
    ) : ModelDownloadRepository {
        var enqueueCalled = false
        override suspend fun getDownloadByFileId(fileId: Long): ModelDownload? =
            existingByFileId[fileId]
        override suspend fun enqueueDownload(download: ModelDownload): Long {
            enqueueCalled = true
            return newId
        }
        override fun observeAllDownloads(): Flow<List<ModelDownload>> = flowOf(emptyList())
        override fun observeDownloadsForModel(modelId: Long): Flow<List<ModelDownload>> =
            flowOf(emptyList())
        override suspend fun getDownloadById(id: Long): ModelDownload? = null
        override suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String?) =
            Unit
        override suspend fun updateProgress(id: Long, downloadedBytes: Long) = Unit
        override suspend fun updateDestinationPath(id: Long, path: String) = Unit
        override suspend fun deleteDownload(id: Long) = Unit
        override suspend fun updateHashVerified(id: Long, verified: Boolean) = Unit
        override suspend fun clearCompletedDownloads() = Unit
    }
}
