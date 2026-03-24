package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.Model

interface TensorArtRepository {
    suspend fun searchModels(
        query: String = "",
        page: Int = 1,
        pageSize: Int = 20,
    ): List<Model>
}
