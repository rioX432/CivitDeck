package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow

class ObserveNetworkStatusUseCase(private val repository: NetworkRepository) {
    operator fun invoke(): Flow<Boolean> = repository.isOnline
}
