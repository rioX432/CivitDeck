package com.riox432.civitdeck.feature.search.domain.usecase

import com.riox432.civitdeck.domain.model.BaseModelCatalog
import com.riox432.civitdeck.domain.repository.BaseModelCatalogRepository
import kotlinx.coroutines.flow.Flow

class ObserveBaseModelCatalogUseCase(private val repository: BaseModelCatalogRepository) {
    operator fun invoke(): Flow<BaseModelCatalog> = repository.observeCatalog()
}
