package com.omooooori.civitdeck.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreatorListResponse(
    val items: List<CreatorDto>,
    val metadata: PaginationMetadataDto,
)

@Serializable
data class CreatorDto(
    val username: String,
    val modelCount: Int = 0,
    val link: String? = null,
)
