package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.LocalModelFile
import com.riox432.civitdeck.domain.model.ModelDirectory
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface LocalModelFileRepository {
    fun observeDirectories(): Flow<List<ModelDirectory>>
    suspend fun addDirectory(path: String, label: String?): Long
    suspend fun removeDirectory(id: Long)

    fun observeLocalFiles(): Flow<List<LocalModelFile>>
    suspend fun scanDirectory(
        directoryId: Long,
        onProgress: (current: Int, total: Int) -> Unit,
    )

    suspend fun verifyFileHash(fileId: Long, sha256Hash: String)

    fun observeOwnedHashes(): Flow<Set<String>>
    suspend fun getOwnedHashes(): Set<String>

    fun observeFileCount(): Flow<Int>
    fun observeMatchedCount(): Flow<Int>
    fun observeUpdatesAvailableCount(): Flow<Int>
}
