package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository

class EnqueueDownloadUseCase(private val repository: ModelDownloadRepository) {
    suspend operator fun invoke(download: ModelDownload): Long {
        val existing = repository.getDownloadByFileId(download.fileId)
        if (existing != null) return existing.id
        return repository.enqueueDownload(download)
    }
}
