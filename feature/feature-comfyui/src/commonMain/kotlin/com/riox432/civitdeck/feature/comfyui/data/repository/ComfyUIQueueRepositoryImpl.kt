package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.local.dao.ComfyUIConnectionDao
import com.riox432.civitdeck.util.Logger
import com.riox432.civitdeck.domain.model.QueueJob
import com.riox432.civitdeck.domain.model.QueueJobStatus
import com.riox432.civitdeck.domain.repository.ComfyUIQueueRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "ComfyUIQueueRepo"

class ComfyUIQueueRepositoryImpl(
    private val dao: ComfyUIConnectionDao,
    private val api: ComfyUIApi,
) : ComfyUIQueueRepository {

    override fun observeQueue(intervalMs: Long): Flow<List<QueueJob>> = flow {
        while (true) {
            try {
                ensureApiConfigured()
                val response = api.getQueue()
                val jobs = mutableListOf<QueueJob>()
                response.running.forEachIndexed { index, entry ->
                    jobs.add(QueueJob(extractPromptId(entry), index, QueueJobStatus.Running))
                }
                response.pending.forEachIndexed { index, entry ->
                    jobs.add(QueueJob(extractPromptId(entry), index, QueueJobStatus.Queued))
                }
                emit(jobs)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w(TAG, "Failed to poll queue: ${e.message}")
                emit(emptyList())
            }
            delay(intervalMs)
        }
    }

    override suspend fun cancelJob(promptId: String) {
        ensureApiConfigured()
        api.deleteQueue(listOf(promptId))
    }

    /**
     * ComfyUI queue entries are arrays: [queue_number, prompt_id, prompt, extra_data, outputs].
     * Extract prompt_id (index 1) from the array element.
     */
    private fun extractPromptId(entry: JsonElement): String {
        return try {
            when (entry) {
                is JsonArray -> entry.getOrNull(1)?.jsonPrimitive?.content ?: ""
                is JsonObject -> entry["prompt_id"]?.jsonPrimitive?.content ?: ""
                else -> entry.jsonPrimitive.content
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Failed to extract prompt ID: ${e.message}")
            ""
        }
    }

    private suspend fun ensureApiConfigured() {
        val active = dao.getActive() ?: error("No active ComfyUI connection")
        api.setBaseUrl(active.hostname, active.port)
    }
}
