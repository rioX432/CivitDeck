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
    val cloudKey: String = "",
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

// Generation models

data class GenerationOption(
    val key: String,
    val label: String,
    val type: GenerationOptionType,
    val choices: List<GenerationChoice> = emptyList(),
    val dependsOn: String? = null,
    val choicesEndpoint: String? = null,
    val placeholder: String? = null,
    val defaultValue: String? = null,
    val min: Int? = null,
    val max: Int? = null,
)

enum class GenerationOptionType {
    SELECT,
    TEXT,
    NUMBER,
    ;

    companion object {
        fun fromString(value: String): GenerationOptionType = when (value) {
            "select" -> SELECT
            "text" -> TEXT
            "number" -> NUMBER
            else -> TEXT
        }
    }
}

data class GenerationChoice(
    val value: String,
    val label: String,
    val description: String? = null,
)

data class GenerationJob(
    val jobId: String,
    val status: GenerationJobStatus,
    val progress: Float = 0f,
    val completed: Int = 0,
    val total: Int = 0,
    val message: String = "",
)

enum class GenerationJobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    ERROR,
    ;

    companion object {
        fun fromString(value: String): GenerationJobStatus = when (value) {
            "queued" -> QUEUED
            "running" -> RUNNING
            "completed" -> COMPLETED
            "error" -> ERROR
            else -> ERROR
        }
    }
}
