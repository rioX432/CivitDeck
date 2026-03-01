package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository

class SetDefaultSortOrderUseCase(private val repository: DisplayPreferencesRepository) {
    suspend operator fun invoke(sort: SortOrder) = repository.setDefaultSortOrder(sort)
}
