package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.BackupCategory
import com.riox432.civitdeck.domain.model.RestoreStrategy

interface BackupRepository {
    suspend fun createBackup(categories: Set<BackupCategory>): String
    suspend fun restoreBackup(json: String, strategy: RestoreStrategy, categories: Set<BackupCategory>)
    suspend fun parseBackupCategories(json: String): Set<BackupCategory>
}
