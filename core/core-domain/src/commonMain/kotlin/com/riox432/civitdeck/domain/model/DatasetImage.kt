package com.riox432.civitdeck.domain.model

data class DatasetImage(
    val id: Long,
    val datasetId: Long,
    val imageUrl: String,
    val sourceType: ImageSource,
    val trainable: Boolean = true,
    val addedAt: Long,
    val tags: List<ImageTag> = emptyList(),
    val caption: Caption? = null,
    val licenseNote: String? = null,
    val pHash: String? = null,
    val excluded: Boolean = false,
    val width: Int? = null,
    val height: Int? = null,
)
