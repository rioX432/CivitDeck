package com.riox432.civitdeck.feature.externalserver.domain.model

data class ServerCapabilities(
    val endpoints: List<String> = emptyList(),
    val name: String = "",
    val version: String = "",
) {
    fun supports(endpoint: String): Boolean = endpoint in endpoints
}

data class ServerImage(
    val id: Int,
    val file: String,
    val thumbUrl: String?,
    val character: String?,
    val costume: String?,
    val scenario: String?,
    val nsfw: Boolean,
    val selected: Boolean,
    val postStatus: String?,
    val aestheticScore: Float?,
    val createdAt: String?,
    val seed: Long?,
    val prompt: String?,
)

data class PaginatedImagesResponse(
    val images: List<ServerImage>,
    val total: Int,
    val page: Int,
    val perPage: Int,
    val totalPages: Int,
)

data class ExternalServerImageFilters(
    val character: String = "",
    val scenario: String = "",
    val nsfw: String = "",
    val status: String = "",
    val sort: String = "newest",
    val search: String = "",
) {
    fun toMap(): Map<String, String> = buildMap {
        if (character.isNotBlank()) put("character", character)
        if (scenario.isNotBlank()) put("scenario", scenario)
        if (nsfw.isNotBlank()) put("nsfw", nsfw)
        if (status.isNotBlank()) put("status", status)
        if (sort.isNotBlank()) put("sort", sort)
        if (search.isNotBlank()) put("search", search)
    }
}
