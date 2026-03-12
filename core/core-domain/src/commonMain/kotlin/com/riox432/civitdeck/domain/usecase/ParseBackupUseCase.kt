package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.BackupCategory
import com.riox432.civitdeck.domain.repository.BackupRepository

class ParseBackupUseCase(private val repository: BackupRepository) {
    suspend operator fun invoke(json: String): Set<BackupCategory> =
        repository.parseBackupCategories(json)
}
