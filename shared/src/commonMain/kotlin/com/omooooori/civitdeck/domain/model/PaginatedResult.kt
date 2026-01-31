package com.omooooori.civitdeck.domain.model

data class PaginatedResult<T>(
    val items: List<T>,
    val metadata: PageMetadata,
)

data class PageMetadata(
    val totalItems: Int?,
    val currentPage: Int?,
    val pageSize: Int?,
    val totalPages: Int?,
    val nextPage: String?,
    val prevPage: String?,
    val nextCursor: String?,
)
