package com.riox432.civitdeck.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The base model arrays of `GET /api/v1/enums`. The response also carries `ModelType`,
 * `ModelFileType` and `BaseModelType`, which are not read yet.
 */
@Serializable
data class EnumsResponse(
    @SerialName("ActiveBaseModel") val activeBaseModels: List<String>,
    @SerialName("BaseModel") val baseModels: List<String>,
)
