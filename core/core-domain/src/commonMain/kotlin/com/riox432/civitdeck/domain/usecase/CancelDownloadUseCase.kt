package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository

class CancelDownloadUseCase(private val repository: ModelDownloadRepository) {
    suspend operator fun invoke(id: Long) {
        repository.updateStatus(id, DownloadStatus.Cancelled)
    }
}
