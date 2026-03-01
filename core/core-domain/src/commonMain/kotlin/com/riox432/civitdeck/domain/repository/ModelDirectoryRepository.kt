package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ModelDirectory
import kotlinx.coroutines.flow.Flow

interface ModelDirectoryRepository {
    fun observeDirectories(): Flow<List<ModelDirectory>>
    suspend fun addDirectory(path: String, label: String?): Long
    suspend fun removeDirectory(id: Long)
}
