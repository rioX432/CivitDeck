package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.LocalModelFileDao
import com.riox432.civitdeck.data.local.entity.LocalModelFileEntity
import com.riox432.civitdeck.data.local.entity.ModelDirectoryEntity
import com.riox432.civitdeck.data.scanner.FileScanner
import com.riox432.civitdeck.domain.model.LocalModelFile
import com.riox432.civitdeck.domain.model.MatchedModelInfo
import com.riox432.civitdeck.domain.model.ModelDirectory
import com.riox432.civitdeck.domain.repository.LocalModelFileRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalModelFileRepositoryImpl(
    private val dao: LocalModelFileDao,
    private val api: CivitAiApi,
    private val fileScanner: FileScanner,
) : LocalModelFileRepository {

    override fun observeDirectories(): Flow<List<ModelDirectory>> =
        dao.observeDirectories().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addDirectory(path: String, label: String?): Long =
        dao.insertDirectory(ModelDirectoryEntity(path = path, label = label))

    override suspend fun removeDirectory(id: Long) {
        dao.deleteFilesByDirectory(id)
        dao.deleteDirectory(id)
    }

    override fun observeLocalFiles(): Flow<List<LocalModelFile>> =
        dao.observeAllFiles().map { entities -> entities.map { it.toDomain() } }

    override suspend fun scanDirectory(
        directoryId: Long,
        onProgress: (current: Int, total: Int) -> Unit,
    ) {
        val directories = if (directoryId == SCAN_ALL_DIRECTORIES) {
            dao.getEnabledDirectories()
        } else {
            listOf(dao.getEnabledDirectories().first { it.id == directoryId })
        }

        for (directory in directories) {
            dao.deleteFilesByDirectory(directory.id)
            val scannedFiles = fileScanner.scanDirectory(directory.path, onProgress)
            val now = currentTimeMillis()
            val entities = scannedFiles.map { file ->
                LocalModelFileEntity(
                    directoryId = directory.id,
                    filePath = file.filePath,
                    fileName = file.fileName,
                    sha256Hash = file.sha256Hash,
                    sizeBytes = file.sizeBytes,
                    scannedAt = now,
                )
            }
            dao.insertFiles(entities)
            dao.updateLastScannedAt(directory.id, now)
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun verifyFileHash(fileId: Long, sha256Hash: String) {
        try {
            currentCoroutineContext().ensureActive()
            val versionResponse = api.getModelVersionByHash(sha256Hash)
            val modelResponse = api.getModel(versionResponse.modelId)
            val latestVersionId = modelResponse.modelVersions.firstOrNull()?.id
            dao.updateMatchInfo(
                fileId = fileId,
                modelId = versionResponse.modelId,
                modelName = modelResponse.name,
                versionId = versionResponse.id,
                versionName = versionResponse.name,
                latestVersionId = latestVersionId,
                hasUpdate = latestVersionId != null && latestVersionId != versionResponse.id,
            )
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (_: Exception) {
            // Hash not found in CivitAI or network error — leave unmatched
        }
    }

    override fun observeOwnedHashes(): Flow<Set<String>> =
        dao.observeOwnedHashes().map { it.toSet() }

    override suspend fun getOwnedHashes(): Set<String> =
        dao.getOwnedHashes().toSet()

    override fun observeFileCount(): Flow<Int> = dao.observeFileCount()

    override fun observeMatchedCount(): Flow<Int> = dao.observeMatchedCount()

    override fun observeUpdatesAvailableCount(): Flow<Int> = dao.observeUpdatesAvailableCount()

    companion object {
        internal const val SCAN_ALL_DIRECTORIES = -1L
    }
}

private fun ModelDirectoryEntity.toDomain() = ModelDirectory(
    id = id,
    path = path,
    label = label,
    lastScannedAt = lastScannedAt,
    isEnabled = isEnabled,
)

private fun LocalModelFileEntity.toDomain() = LocalModelFile(
    id = id,
    directoryId = directoryId,
    filePath = filePath,
    fileName = fileName,
    sha256Hash = sha256Hash,
    sizeBytes = sizeBytes,
    scannedAt = scannedAt,
    matchedModel = matchedModelId?.let { modelId ->
        MatchedModelInfo(
            modelId = modelId,
            modelName = matchedModelName ?: "Unknown",
            versionId = matchedVersionId ?: 0,
            versionName = matchedVersionName ?: "Unknown",
            latestVersionId = latestVersionId,
            hasUpdate = hasUpdate,
        )
    },
)
