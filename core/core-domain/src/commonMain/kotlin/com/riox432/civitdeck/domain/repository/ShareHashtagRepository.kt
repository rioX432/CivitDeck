package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ShareHashtag
import kotlinx.coroutines.flow.Flow

interface ShareHashtagRepository {
    fun observeAll(): Flow<List<ShareHashtag>>
    suspend fun addCustom(tag: String)
    suspend fun remove(tag: String)
    suspend fun setEnabled(tag: String, isEnabled: Boolean)
}
