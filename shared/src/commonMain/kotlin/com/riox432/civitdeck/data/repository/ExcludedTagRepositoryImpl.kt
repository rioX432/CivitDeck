package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.ExcludedTagDao
import com.riox432.civitdeck.data.local.entity.ExcludedTagEntity
import com.riox432.civitdeck.domain.repository.ExcludedTagRepository

class ExcludedTagRepositoryImpl(
    private val dao: ExcludedTagDao,
) : ExcludedTagRepository {

    override suspend fun getExcludedTags(): List<String> =
        dao.getAll().map { it.tag }

    override suspend fun addExcludedTag(tag: String) {
        dao.insert(ExcludedTagEntity(tag = tag, addedAt = currentTimeMillis()))
    }

    override suspend fun removeExcludedTag(tag: String) {
        dao.delete(tag)
    }
}
