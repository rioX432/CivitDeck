package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.QueueJob
import com.riox432.civitdeck.domain.model.QueueJobStatus
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [ComfyUIQueueRepositoryImpl]: queue polling maps running/pending entries
 * to [com.riox432.civitdeck.domain.model.QueueJob]s, the guard requiring an active
 * connection, and the error branch emitting an empty list.
 */
class ComfyUIQueueRepositoryImplTest {

    private fun daoWithActive() = FakeComfyUIConnectionDao().apply {
        rows.add(ComfyUIConnectionEntity(id = 1, name = "A", hostname = "h", port = 8188, isActive = true, createdAt = 1))
    }

    private fun api(body: String) = ComfyUIApi(mockClient { okJson(body) }, testJson)

    /**
     * Captures the first emission of an infinite polling flow without using
     * `Flow.first()`. The repo's `while(true)` loop has a generic `catch` that
     * re-emits an empty list; cancelling from inside the flow (as `first()` does)
     * re-enters that catch and trips flow-transparency. Instead we cancel the
     * collecting job externally after the first value.
     */
    private suspend fun firstEmission(flow: Flow<List<QueueJob>>): List<QueueJob> {
        var captured: List<QueueJob>? = null
        kotlinx.coroutines.coroutineScope {
            val job = flow.onEach {
                captured = it
            }.launchIn(this)
            while (captured == null) kotlinx.coroutines.yield()
            job.cancel()
        }
        return captured ?: emptyList()
    }

    @Test
    fun observeQueue_maps_running_and_pending_entries_with_status() = runTest {
        // ComfyUI queue entries are arrays: [queue_number, prompt_id, ...].
        val body = """
            {"queue_running":[[0,"run-1"]],"queue_pending":[[1,"pend-1"],[2,"pend-2"]]}
        """.trimIndent()
        val repo = ComfyUIQueueRepositoryImpl(daoWithActive(), api(body))

        val jobs = firstEmission(repo.observeQueue(intervalMs = 1000))

        assertEquals(3, jobs.size)
        assertEquals("run-1", jobs[0].promptId)
        assertEquals(QueueJobStatus.Running, jobs[0].status)
        assertEquals(QueueJobStatus.Queued, jobs[1].status)
        assertEquals("pend-2", jobs[2].promptId)
    }

    @Test
    fun observeQueue_emits_empty_when_no_active_connection() = runTest {
        // No active connection -> ensureApiConfigured throws -> caught -> empty list emitted.
        val repo = ComfyUIQueueRepositoryImpl(FakeComfyUIConnectionDao(), api("{}"))

        val jobs = firstEmission(repo.observeQueue(intervalMs = 1000))

        assertTrue(jobs.isEmpty())
    }

    @Test
    fun observeQueue_emits_empty_on_api_error() = runTest {
        val errorApi = ComfyUIApi(
            mockClient { respondError(HttpStatusCode.InternalServerError) },
            testJson,
        )
        val repo = ComfyUIQueueRepositoryImpl(daoWithActive(), errorApi)

        val jobs = firstEmission(repo.observeQueue(intervalMs = 1000))

        assertTrue(jobs.isEmpty())
    }

    @Test
    fun cancelJob_posts_delete_request_for_prompt_id() = runTest {
        var deleteHit = false
        val client = mockClient { req ->
            if (req.url.encodedPath == "/queue") deleteHit = true
            okJson("{}")
        }
        val repo = ComfyUIQueueRepositoryImpl(daoWithActive(), ComfyUIApi(client, testJson))

        repo.cancelJob("abc")

        assertTrue(deleteHit)
    }
}
