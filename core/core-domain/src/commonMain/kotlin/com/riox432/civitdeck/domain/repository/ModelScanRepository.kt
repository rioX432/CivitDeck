package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.LocalModelFile
import kotlinx.coroutines.flow.Flow

interface ModelScanRepository {
    fun observeLocalFiles(): Flow<List<LocalModelFile>>
    suspend fun scanDirectory(
        directoryId: Long,
        onProgress: (current: Int, total: Int) -> Unit,
    )
}
