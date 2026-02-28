package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.UserPreferencesRepository

class SetGridColumnsUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(columns: Int) = repository.setGridColumns(columns)
}
