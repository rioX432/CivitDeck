package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.CivitaiLinkActivity
import com.riox432.civitdeck.domain.model.CivitaiLinkResource
import com.riox432.civitdeck.domain.model.CivitaiLinkStatus
import kotlinx.coroutines.flow.Flow

interface CivitaiLinkRepository {
    fun observeStatus(): Flow<CivitaiLinkStatus>
    fun observeActivities(): Flow<List<CivitaiLinkActivity>>
    suspend fun connect(key: String)
    fun disconnect()
    suspend fun sendResourceToPC(resource: CivitaiLinkResource)
    suspend fun cancelActivity(activityId: String)
    fun isConnected(): Boolean
}
