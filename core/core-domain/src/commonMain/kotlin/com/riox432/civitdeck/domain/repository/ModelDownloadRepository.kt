package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.ModelDownload
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface ModelDownloadRepository {
    suspend fun enqueueDownload(download: ModelDownload): Long
    fun observeAllDownloads(): Flow<List<ModelDownload>>
    fun observeDownloadsForModel(modelId: Long): Flow<List<ModelDownload>>
    suspend fun getDownloadById(id: Long): ModelDownload?
    suspend fun getDownloadByFileId(fileId: Long): ModelDownload?
    suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String? = null)
    suspend fun updateProgress(id: Long, downloadedBytes: Long)
    suspend fun updateDestinationPath(id: Long, path: String)
    suspend fun deleteDownload(id: Long)
    suspend fun updateHashVerified(id: Long, verified: Boolean)
    suspend fun clearCompletedDownloads()
}
