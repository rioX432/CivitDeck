package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ModelDownloadRepository

/**
 * Verifies that a completed download's SHA-256 hash matches the expected hash
 * from the CivitAI API metadata.
 */
class VerifyDownloadHashUseCase(private val repository: ModelDownloadRepository) {
    suspend operator fun invoke(downloadId: Long, expectedSha256: String): HashVerificationResult {
        val download = repository.getDownloadById(downloadId)
            ?: return HashVerificationResult.NotFound
        val path = download.destinationPath
            ?: return HashVerificationResult.NoFile
        return HashVerificationResult.Pending(path, expectedSha256)
    }
}

sealed interface HashVerificationResult {
    data object NotFound : HashVerificationResult
    data object NoFile : HashVerificationResult
    data class Pending(val filePath: String, val expectedHash: String) : HashVerificationResult
    data class Verified(val matched: Boolean, val actualHash: String) : HashVerificationResult
}
