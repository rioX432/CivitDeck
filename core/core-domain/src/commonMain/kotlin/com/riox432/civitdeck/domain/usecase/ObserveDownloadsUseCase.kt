package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository
import kotlinx.coroutines.flow.Flow

class ObserveDownloadsUseCase(private val repository: ModelDownloadRepository) {
    operator fun invoke(): Flow<List<ModelDownload>> = repository.observeAllDownloads()
}
