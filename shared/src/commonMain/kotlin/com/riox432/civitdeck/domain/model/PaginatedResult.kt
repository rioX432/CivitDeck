package com.riox432.civitdeck.domain.model

data class PaginatedResult<T>(
    val items: List<T>,
    val metadata: PageMetadata,
)

data class PageMetadata(
    val nextCursor: String?,
    val nextPage: String?,
)
