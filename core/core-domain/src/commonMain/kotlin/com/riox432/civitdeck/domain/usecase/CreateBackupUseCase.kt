package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.BackupCategory
import com.riox432.civitdeck.domain.repository.BackupRepository

class CreateBackupUseCase(private val repository: BackupRepository) {
    suspend operator fun invoke(categories: Set<BackupCategory>): String =
        repository.createBackup(categories)
}
