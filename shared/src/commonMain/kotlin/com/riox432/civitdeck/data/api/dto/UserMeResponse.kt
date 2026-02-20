package com.riox432.civitdeck.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserMeResponse(
    val id: Long,
    val username: String,
    val image: String? = null,
)
