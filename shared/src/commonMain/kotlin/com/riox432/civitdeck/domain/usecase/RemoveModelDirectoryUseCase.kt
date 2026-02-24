package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.LocalModelFileRepository

class RemoveModelDirectoryUseCase(private val repository: LocalModelFileRepository) {
    suspend operator fun invoke(id: Long) = repository.removeDirectory(id)
}
