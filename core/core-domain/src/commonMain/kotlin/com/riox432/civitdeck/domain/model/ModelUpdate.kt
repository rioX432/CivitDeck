package com.riox432.civitdeck.domain.model

data class ModelUpdate(
    val modelId: Long,
    val modelName: String,
    val newVersionName: String,
    val newVersionId: Long,
    val source: UpdateSource = UpdateSource.FAVORITE,
)
