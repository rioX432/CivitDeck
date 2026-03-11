package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ModelDownloadRepository

class DeleteDownloadUseCase(private val repository: ModelDownloadRepository) {
    suspend operator fun invoke(id: Long) {
        repository.deleteDownload(id)
    }
}
