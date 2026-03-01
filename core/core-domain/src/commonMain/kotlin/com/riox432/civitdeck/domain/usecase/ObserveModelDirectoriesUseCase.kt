package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelDirectory
import com.riox432.civitdeck.domain.repository.ModelDirectoryRepository
import kotlinx.coroutines.flow.Flow

class ObserveModelDirectoriesUseCase(private val repository: ModelDirectoryRepository) {
    operator fun invoke(): Flow<List<ModelDirectory>> = repository.observeDirectories()
}
