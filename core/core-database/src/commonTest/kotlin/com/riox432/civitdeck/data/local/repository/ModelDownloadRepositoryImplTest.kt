package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.ModelDownloadDao
import com.riox432.civitdeck.data.local.entity.ModelDownloadEntity
import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.ModelDownload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [ModelDownloadRepositoryImpl] verifying enqueue, status/progress updates,
 * lookups, completed-cleanup, and entity-to-domain mapping via a hand-written fake DAO.
 */
class ModelDownloadRepositoryImplTest {

    private class FakeDao : ModelDownloadDao {
        val entities = mutableListOf<ModelDownloadEntity>()
        private var idCounter = 1L
        private val flow = MutableStateFlow<List<ModelDownloadEntity>>(emptyList())

        private fun emit() {
            flow.value = entities.sortedByDescending { it.createdAt }.toList()
        }

        override suspend fun insert(entity: ModelDownloadEntity): Long {
            val id = idCounter++
            entities.add(entity.copy(id = id))
            emit()
            return id
        }

        override fun observeAll(): Flow<List<ModelDownloadEntity>> = flow

        override fun observeByModelId(modelId: Long): Flow<List<ModelDownloadEntity>> =
            MutableStateFlow(entities.filter { it.modelId == modelId })

        override suspend fun getById(id: Long): ModelDownloadEntity? =
            entities.firstOrNull { it.id == id }

        override suspend fun getByFileId(fileId: Long): ModelDownloadEntity? =
            entities.firstOrNull { it.fileId == fileId }

        override suspend fun updateStatus(id: Long, status: String, updatedAt: Long): Int =
            mutate(id) { it.copy(status = status, updatedAt = updatedAt) }

        override suspend fun updateProgress(id: Long, bytes: Long, updatedAt: Long): Int =
            mutate(id) { it.copy(downloadedBytes = bytes, updatedAt = updatedAt) }

        override suspend fun updateDestinationPath(id: Long, path: String, updatedAt: Long): Int =
            mutate(id) { it.copy(destinationPath = path, updatedAt = updatedAt) }

        override suspend fun delete(id: Long): Int {
            val before = entities.size
            entities.removeAll { it.id == id }
            emit()
            return before - entities.size
        }

        override suspend fun updateHashVerified(id: Long, verified: Int, updatedAt: Long): Int =
            mutate(id) { it.copy(hashVerified = verified, updatedAt = updatedAt) }

        override suspend fun deleteCompleted(): Int {
            val before = entities.size
            entities.removeAll { it.status == "Completed" }
            emit()
            return before - entities.size
        }

        private fun mutate(id: Long, block: (ModelDownloadEntity) -> ModelDownloadEntity): Int {
            val idx = entities.indexOfFirst { it.id == id }
            if (idx < 0) return 0
            entities[idx] = block(entities[idx])
            emit()
            return 1
        }
    }

    private fun sampleDownload(modelId: Long = 1L, fileId: Long = 100L) = ModelDownload(
        modelId = modelId,
        modelName = "Test",
        versionId = 10L,
        versionName = "v1",
        fileId = fileId,
        fileName = "model.safetensors",
        fileUrl = "https://example.com/m",
        fileSizeBytes = 2048L,
        status = DownloadStatus.Pending,
        modelType = "LORA",
        expectedSha256 = "abc",
    )

    @Test
    fun enqueueDownload_inserts_entity_and_returns_id() = runTest {
        val dao = FakeDao()
        val repo = ModelDownloadRepositoryImpl(dao)
        val id = repo.enqueueDownload(sampleDownload())
        assertEquals(1L, id)
        assertEquals(1, dao.entities.size)
        assertEquals("Pending", dao.entities[0].status)
    }

    @Test
    fun observeAllDownloads_maps_entities_to_domain() = runTest {
        val dao = FakeDao()
        val repo = ModelDownloadRepositoryImpl(dao)
        repo.enqueueDownload(sampleDownload())
        val result = repo.observeAllDownloads().first()
        assertEquals(1, result.size)
        assertEquals(DownloadStatus.Pending, result[0].status)
        assertEquals("model.safetensors", result[0].fileName)
    }

    @Test
    fun getDownloadByFileId_finds_matching_download() = runTest {
        val dao = FakeDao()
        val repo = ModelDownloadRepositoryImpl(dao)
        repo.enqueueDownload(sampleDownload(fileId = 100L))
        repo.enqueueDownload(sampleDownload(fileId = 200L))
        assertEquals(100L, repo.getDownloadByFileId(100L)?.fileId)
        assertNull(repo.getDownloadByFileId(999L))
    }

    @Test
    fun updateStatus_changes_status() = runTest {
        val dao = FakeDao()
        val repo = ModelDownloadRepositoryImpl(dao)
        val id = repo.enqueueDownload(sampleDownload())
        repo.updateStatus(id, DownloadStatus.Downloading, null)
        assertEquals(DownloadStatus.Downloading, repo.getDownloadById(id)?.status)
    }

    @Test
    fun updateProgress_sets_downloaded_bytes() = runTest {
        val dao = FakeDao()
        val repo = ModelDownloadRepositoryImpl(dao)
        val id = repo.enqueueDownload(sampleDownload())
        repo.updateProgress(id, 512L)
        assertEquals(512L, repo.getDownloadById(id)?.downloadedBytes)
    }

    @Test
    fun updateHashVerified_maps_int_to_boolean() = runTest {
        val dao = FakeDao()
        val repo = ModelDownloadRepositoryImpl(dao)
        val id = repo.enqueueDownload(sampleDownload())
        repo.updateHashVerified(id, true)
        assertEquals(true, repo.getDownloadById(id)?.hashVerified)
        assertEquals(1, dao.entities[0].hashVerified)
    }

    @Test
    fun deleteDownload_removes_entity() = runTest {
        val dao = FakeDao()
        val repo = ModelDownloadRepositoryImpl(dao)
        val id = repo.enqueueDownload(sampleDownload())
        repo.deleteDownload(id)
        assertTrue(dao.entities.isEmpty())
    }

    @Test
    fun clearCompletedDownloads_removes_only_completed() = runTest {
        val dao = FakeDao()
        val repo = ModelDownloadRepositoryImpl(dao)
        val keepId = repo.enqueueDownload(sampleDownload())
        val doneId = repo.enqueueDownload(sampleDownload(modelId = 2L))
        repo.updateStatus(doneId, DownloadStatus.Completed, null)
        repo.clearCompletedDownloads()
        assertEquals(1, dao.entities.size)
        assertEquals(keepId, dao.entities[0].id)
    }
}
