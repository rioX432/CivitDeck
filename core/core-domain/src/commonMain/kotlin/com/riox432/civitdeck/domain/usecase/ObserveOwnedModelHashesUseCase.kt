package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ModelFileHashRepository
import kotlinx.coroutines.flow.Flow

class ObserveOwnedModelHashesUseCase(private val repository: ModelFileHashRepository) {
    operator fun invoke(): Flow<Set<String>> = repository.observeOwnedHashes()
}
