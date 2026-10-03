package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.BaseModelCatalog
import kotlinx.coroutines.flow.Flow

interface BaseModelCatalogRepository {
    fun observeCatalog(): Flow<BaseModelCatalog>
}
