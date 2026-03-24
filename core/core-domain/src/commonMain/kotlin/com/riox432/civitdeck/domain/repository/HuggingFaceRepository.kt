package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.Model

interface HuggingFaceRepository {
    suspend fun searchModels(
        query: String? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): List<Model>
}
