package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository

class SetDefaultSortOrderUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(sort: SortOrder) = repository.setDefaultSortOrder(sort)
}
