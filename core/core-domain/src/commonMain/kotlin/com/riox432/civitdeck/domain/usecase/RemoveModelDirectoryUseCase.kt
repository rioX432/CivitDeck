package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ModelDirectoryRepository

class RemoveModelDirectoryUseCase(private val repository: ModelDirectoryRepository) {
    suspend operator fun invoke(id: Long) = repository.removeDirectory(id)
}
