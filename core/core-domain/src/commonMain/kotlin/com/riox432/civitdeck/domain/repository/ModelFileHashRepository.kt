package com.riox432.civitdeck.domain.repository

import kotlinx.coroutines.flow.Flow

interface ModelFileHashRepository {
    suspend fun verifyFileHash(fileId: Long, sha256Hash: String)
    fun observeOwnedHashes(): Flow<Set<String>>
    suspend fun getOwnedHashes(): Set<String>
    fun observeFileCount(): Flow<Int>
    fun observeMatchedCount(): Flow<Int>
    fun observeUpdatesAvailableCount(): Flow<Int>
}
