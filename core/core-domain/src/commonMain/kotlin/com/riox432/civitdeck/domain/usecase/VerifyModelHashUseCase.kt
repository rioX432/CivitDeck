package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ModelFileHashRepository

class VerifyModelHashUseCase(private val repository: ModelFileHashRepository) {
    suspend operator fun invoke(fileId: Long, sha256Hash: String) =
        repository.verifyFileHash(fileId, sha256Hash)
}
