package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ExportFormat
import com.riox432.civitdeck.domain.model.ExportProgress
import com.riox432.civitdeck.domain.repository.ExportRepository
import kotlinx.coroutines.flow.Flow

class ExportDatasetUseCase(private val repository: ExportRepository) {
    operator fun invoke(datasetId: Long, format: ExportFormat = ExportFormat.ZIP): Flow<ExportProgress> =
        repository.exportDataset(datasetId, format)
}
