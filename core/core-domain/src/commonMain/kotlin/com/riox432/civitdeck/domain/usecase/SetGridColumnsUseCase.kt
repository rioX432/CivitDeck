package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository

class SetGridColumnsUseCase(private val repository: DisplayPreferencesRepository) {
    suspend operator fun invoke(columns: Int) = repository.setGridColumns(columns)
}
