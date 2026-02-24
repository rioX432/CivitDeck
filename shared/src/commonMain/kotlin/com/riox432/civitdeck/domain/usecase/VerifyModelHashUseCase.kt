package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.LocalModelFileRepository

class VerifyModelHashUseCase(private val repository: LocalModelFileRepository) {
    suspend operator fun invoke(fileId: Long, sha256Hash: String) =
        repository.verifyFileHash(fileId, sha256Hash)
}
