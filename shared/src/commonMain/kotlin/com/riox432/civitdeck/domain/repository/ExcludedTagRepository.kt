package com.riox432.civitdeck.domain.repository

interface ExcludedTagRepository {
    suspend fun getExcludedTags(): List<String>
    suspend fun addExcludedTag(tag: String)
    suspend fun removeExcludedTag(tag: String)
}
