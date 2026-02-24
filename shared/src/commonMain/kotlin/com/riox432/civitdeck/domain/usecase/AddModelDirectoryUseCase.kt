package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.LocalModelFileRepository

class AddModelDirectoryUseCase(private val repository: LocalModelFileRepository) {
    suspend operator fun invoke(path: String, label: String? = null): Long =
        repository.addDirectory(path, label)
}
