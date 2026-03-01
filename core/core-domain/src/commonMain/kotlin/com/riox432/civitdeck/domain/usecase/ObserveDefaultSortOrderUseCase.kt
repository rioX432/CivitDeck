package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveDefaultSortOrderUseCase(private val repository: DisplayPreferencesRepository) {
    operator fun invoke(): Flow<SortOrder> = repository.observeDefaultSortOrder()
}
