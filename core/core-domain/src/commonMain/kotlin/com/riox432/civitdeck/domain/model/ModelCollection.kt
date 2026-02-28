package com.riox432.civitdeck.domain.model

data class ModelCollection(
    val id: Long,
    val name: String,
    val isDefault: Boolean,
    val modelCount: Int = 0,
    val thumbnailUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
