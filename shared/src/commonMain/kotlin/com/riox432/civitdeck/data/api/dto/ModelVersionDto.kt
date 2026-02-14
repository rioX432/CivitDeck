package com.riox432.civitdeck.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModelVersionResponse(
    val id: Long,
    val modelId: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: String? = null,
    val baseModel: String? = null,
    val trainedWords: List<String> = emptyList(),
    val downloadUrl: String = "",
    val files: List<ModelFileDto> = emptyList(),
    val images: List<ModelImageDto> = emptyList(),
    val stats: ModelVersionStatsDto? = null,
)

typealias ModelVersionDto = ModelVersionResponse

@Serializable
data class ModelVersionStatsDto(
    val downloadCount: Int = 0,
    val ratingCount: Int = 0,
    val rating: Double = 0.0,
)

@Serializable
data class ModelFileDto(
    val id: Long = 0,
    val name: String = "",
    val sizeKB: Double = 0.0,
    val type: String? = null,
    val metadata: ModelFileMetadataDto? = null,
    val pickleScanResult: String? = null,
    val virusScanResult: String? = null,
    val scannedAt: String? = null,
    val hashes: Map<String, String> = emptyMap(),
    val downloadUrl: String = "",
    val primary: Boolean = false,
)

@Serializable
data class ModelFileMetadataDto(
    val fp: String? = null,
    val size: String? = null,
    val format: String? = null,
)

@Serializable
data class ModelImageDto(
    val url: String,
    val nsfw: Boolean = false,
    val nsfwLevel: Int? = null,
    val width: Int = 0,
    val height: Int = 0,
    val hash: String? = null,
    val meta: ImageMetaDto? = null,
)
