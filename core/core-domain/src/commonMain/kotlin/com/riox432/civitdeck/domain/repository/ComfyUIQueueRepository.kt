package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ComfyUIJobHandle
import com.riox432.civitdeck.domain.model.QueueJob
import kotlinx.coroutines.flow.Flow

interface ComfyUIQueueRepository {
    fun observeQueue(intervalMs: Long = QUEUE_POLL_INTERVAL_MS): Flow<List<QueueJob>>
    suspend fun cancelJob(promptId: String)

    /**
     * Cancel [job] on its own target only. The default fails instead of delegating to
     * [cancelJob] by prompt ID, which acts on the active server.
     */
    suspend fun cancelJob(job: ComfyUIJobHandle): Unit =
        throw UnsupportedOperationException("Bound cancelJob is not implemented")

    companion object {
        const val QUEUE_POLL_INTERVAL_MS = 3000L
    }
}
