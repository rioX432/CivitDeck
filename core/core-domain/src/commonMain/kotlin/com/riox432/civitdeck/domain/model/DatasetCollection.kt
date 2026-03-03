package com.riox432.civitdeck.domain.model

data class DatasetCollection(
    val id: Long,
    val name: String,
    val description: String = "",
    val imageCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)
