package com.riox432.civitdeck.data.api

object CivitAiUrls {
    const val BASE_URL = "https://civitai.com"

    fun modelUrl(modelId: Long) = "$BASE_URL/models/$modelId"

    fun downloadUrl(versionId: Long) = "$BASE_URL/api/download/models/$versionId"
}
