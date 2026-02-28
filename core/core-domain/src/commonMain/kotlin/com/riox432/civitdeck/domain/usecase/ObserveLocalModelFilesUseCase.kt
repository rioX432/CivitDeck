package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.LocalModelFile
import com.riox432.civitdeck.domain.repository.LocalModelFileRepository
import kotlinx.coroutines.flow.Flow

class ObserveLocalModelFilesUseCase(private val repository: LocalModelFileRepository) {
    operator fun invoke(): Flow<List<LocalModelFile>> = repository.observeLocalFiles()
}
