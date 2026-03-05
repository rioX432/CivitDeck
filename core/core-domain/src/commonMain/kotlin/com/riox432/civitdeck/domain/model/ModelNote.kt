package com.riox432.civitdeck.domain.model

data class ModelNote(
    val id: Long = 0,
    val modelId: Long,
    val noteText: String,
    val createdAt: Long,
    val updatedAt: Long,
)
