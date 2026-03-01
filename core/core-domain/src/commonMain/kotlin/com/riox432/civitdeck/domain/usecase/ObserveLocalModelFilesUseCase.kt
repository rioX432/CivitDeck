package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.LocalModelFile
import com.riox432.civitdeck.domain.repository.ModelScanRepository
import kotlinx.coroutines.flow.Flow

class ObserveLocalModelFilesUseCase(private val repository: ModelScanRepository) {
    operator fun invoke(): Flow<List<LocalModelFile>> = repository.observeLocalFiles()
}
