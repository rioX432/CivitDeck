package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveGridColumnsUseCase(private val repository: DisplayPreferencesRepository) {
    operator fun invoke(): Flow<Int> = repository.observeGridColumns()
}
