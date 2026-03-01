package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.QueueJob
import kotlinx.coroutines.flow.Flow

interface ComfyUIQueueRepository {
    fun observeQueue(intervalMs: Long = QUEUE_POLL_INTERVAL_MS): Flow<List<QueueJob>>
    suspend fun cancelJob(promptId: String)

    companion object {
        const val QUEUE_POLL_INTERVAL_MS = 3000L
    }
}
