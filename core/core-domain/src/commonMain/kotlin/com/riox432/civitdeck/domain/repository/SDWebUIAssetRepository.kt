package com.riox432.civitdeck.domain.repository

interface SDWebUIAssetRepository {
    suspend fun fetchModels(): List<String>
    suspend fun fetchSamplers(): List<String>
    suspend fun fetchVaes(): List<String>
}
