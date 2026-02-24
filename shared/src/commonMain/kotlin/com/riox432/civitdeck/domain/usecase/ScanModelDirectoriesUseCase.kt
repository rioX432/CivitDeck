package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.LocalModelFileRepository

class ScanModelDirectoriesUseCase(private val repository: LocalModelFileRepository) {
    suspend operator fun invoke(
        directoryId: Long = SCAN_ALL,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ) = repository.scanDirectory(directoryId, onProgress)

    companion object {
        const val SCAN_ALL = -1L
    }
}
