package com.riox432.civitdeck.data.api.tensorart

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TensorArtSearchRequest(
    val query: String = "",
    val sort: String = "MOST_DOWNLOADED",
    val page: Int = 1,
    val pageSize: Int = 20,
)

@Serializable
data class TensorArtSearchResponse(
    val data: TensorArtSearchData? = null,
    val total: Int = 0,
)

@Serializable
data class TensorArtSearchData(
    val models: List<TensorArtModelDto> = emptyList(),
)

@Serializable
data class TensorArtModelDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val type: String? = null,
    val author: TensorArtAuthorDto? = null,
    val stats: TensorArtStatsDto? = null,
    val coverImage: String? = null,
    val tags: List<String> = emptyList(),
    val baseModel: String? = null,
)

@Serializable
data class TensorArtAuthorDto(
    val name: String? = null,
    @SerialName("avatar")
    val avatar: String? = null,
)

@Serializable
data class TensorArtStatsDto(
    val downloadCount: Int = 0,
    val likeCount: Int = 0,
    val runCount: Int = 0,
)
