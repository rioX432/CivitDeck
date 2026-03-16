package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.ShareHashtagDao
import com.riox432.civitdeck.data.local.entity.ShareHashtagEntity
import com.riox432.civitdeck.domain.model.ShareHashtag
import com.riox432.civitdeck.domain.repository.ShareHashtagRepository
import com.riox432.civitdeck.data.local.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShareHashtagRepositoryImpl(
    private val dao: ShareHashtagDao,
) : ShareHashtagRepository {

    override fun observeAll(): Flow<List<ShareHashtag>> =
        dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun addCustom(tag: String) {
        val normalized = normalizeTag(tag)
        if (normalized.isBlank()) return
        dao.insert(
            ShareHashtagEntity(
                tag = normalized,
                isEnabled = true,
                isCustom = true,
                addedAt = currentTimeMillis(),
            ),
        )
    }

    override suspend fun removeCustom(tag: String) {
        dao.deleteCustom(tag)
    }

    override suspend fun setEnabled(tag: String, isEnabled: Boolean) {
        dao.setEnabled(tag, isEnabled)
    }

    private fun normalizeTag(tag: String): String {
        val trimmed = tag.trim()
        return if (trimmed.startsWith("#")) trimmed else "#$trimmed"
    }

    private fun ShareHashtagEntity.toDomain() = ShareHashtag(
        tag = tag,
        isEnabled = isEnabled,
        isCustom = isCustom,
    )
}
