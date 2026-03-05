package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ExportFormat
import com.riox432.civitdeck.domain.model.ExportProgress
import kotlinx.coroutines.flow.Flow

interface ExportRepository {
    fun exportDataset(datasetId: Long, format: ExportFormat): Flow<ExportProgress>
}
