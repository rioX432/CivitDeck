package com.omooooori.civitdeck.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class TagListResponse(
    val items: List<TagDto>,
    val metadata: PaginationMetadataDto,
)

@Serializable
data class TagDto(
    val name: String,
    val modelCount: Int = 0,
    val link: String? = null,
)
