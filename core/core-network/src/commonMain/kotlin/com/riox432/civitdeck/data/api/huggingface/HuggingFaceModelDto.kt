package com.riox432.civitdeck.data.api.huggingface

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HuggingFaceModelDto(
    @SerialName("modelId") val modelId: String = "",
    val author: String? = null,
    val downloads: Int = 0,
    val likes: Int = 0,
    val tags: List<String> = emptyList(),
    val lastModified: String? = null,
    @SerialName("pipeline_tag") val pipelineTag: String? = null,
    @SerialName("library_name") val libraryName: String? = null,
)
