package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.BackupCategory
import com.riox432.civitdeck.domain.model.RestoreStrategy
import com.riox432.civitdeck.domain.repository.BackupRepository

class RestoreBackupUseCase(private val repository: BackupRepository) {
    suspend operator fun invoke(
        json: String,
        strategy: RestoreStrategy,
        categories: Set<BackupCategory>,
    ) = repository.restoreBackup(json, strategy, categories)
}
