package com.omooooori.civitdeck.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaginationMetadataDto(
    val totalItems: Int? = null,
    val currentPage: Int? = null,
    val pageSize: Int? = null,
    val totalPages: Int? = null,
    val nextPage: String? = null,
    val prevPage: String? = null,
    val nextCursor: String? = null,
)
