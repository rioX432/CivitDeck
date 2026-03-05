package com.riox432.civitdeck.domain.model

data class PersonalTag(
    val id: Long = 0,
    val modelId: Long,
    val tag: String,
    val addedAt: Long,
)
