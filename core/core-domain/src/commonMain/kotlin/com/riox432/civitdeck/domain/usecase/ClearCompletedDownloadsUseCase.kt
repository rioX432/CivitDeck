package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ModelDownloadRepository

class ClearCompletedDownloadsUseCase(private val repository: ModelDownloadRepository) {
    suspend operator fun invoke() {
        repository.clearCompletedDownloads()
    }
}
