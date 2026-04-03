package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.ModelDownloadDao
import com.riox432.civitdeck.data.local.entity.ModelDownloadEntity
import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ModelDownloadRepositoryImpl(
    private val dao: ModelDownloadDao,
) : ModelDownloadRepository {

    override suspend fun enqueueDownload(download: ModelDownload): Long {
        val now = currentTimeMillis()
        val entity = ModelDownloadEntity(
            modelId = download.modelId,
            modelName = download.modelName,
            versionId = download.versionId,
            versionName = download.versionName,
            fileId = download.fileId,
            fileName = download.fileName,
            fileUrl = download.fileUrl,
            fileSizeBytes = download.fileSizeBytes,
            status = download.status.name,
            modelType = download.modelType,
            expectedSha256 = download.expectedSha256,
            createdAt = now,
            updatedAt = now,
        )
        return dao.insert(entity)
    }

    override fun observeAllDownloads(): Flow<List<ModelDownload>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeDownloadsForModel(modelId: Long): Flow<List<ModelDownload>> =
        dao.observeByModelId(modelId).map { list -> list.map { it.toDomain() } }

    override suspend fun getDownloadById(id: Long): ModelDownload? =
        dao.getById(id)?.toDomain()

    override suspend fun getDownloadByFileId(fileId: Long): ModelDownload? =
        dao.getByFileId(fileId)?.toDomain()

    override suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String?) {
        val now = currentTimeMillis()
        dao.updateStatus(id, status.name, now)
    }

    override suspend fun updateProgress(id: Long, downloadedBytes: Long) {
        val now = currentTimeMillis()
        dao.updateProgress(id, downloadedBytes, now)
    }

    override suspend fun updateDestinationPath(id: Long, path: String) {
        val now = currentTimeMillis()
        dao.updateDestinationPath(id, path, now)
    }

    override suspend fun deleteDownload(id: Long) {
        dao.delete(id)
    }

    override suspend fun updateHashVerified(id: Long, verified: Boolean) {
        val now = currentTimeMillis()
        dao.updateHashVerified(id, if (verified) 1 else 0, now)
    }

    override suspend fun clearCompletedDownloads() {
        dao.deleteCompleted()
    }

    private fun ModelDownloadEntity.toDomain() = ModelDownload(
        id = id,
        modelId = modelId,
        modelName = modelName,
        versionId = versionId,
        versionName = versionName,
        fileId = fileId,
        fileName = fileName,
        fileUrl = fileUrl,
        fileSizeBytes = fileSizeBytes,
        downloadedBytes = downloadedBytes,
        status = DownloadStatus.valueOf(status),
        modelType = modelType,
        destinationPath = destinationPath,
        errorMessage = errorMessage,
        expectedSha256 = expectedSha256,
        hashVerified = hashVerified?.let { it == 1 },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
