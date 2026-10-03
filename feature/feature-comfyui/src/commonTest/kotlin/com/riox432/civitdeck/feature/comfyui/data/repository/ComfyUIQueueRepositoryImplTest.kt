package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.QueueJob
import com.riox432.civitdeck.domain.model.QueueJobStatus
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
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
 * connection, the error branch emitting an empty list, and cancelJob choosing between
 * a targeted interrupt (running job) and a queue delete (pending job).
 */
class ComfyUIQueueRepositoryImplTest {

    private fun daoWithActive() = FakeComfyUIConnectionDao().apply {
        rows.add(ComfyUIConnectionEntity(id = 1, name = "A", hostname = "h", port = 8188, isActive = true, createdAt = 1))
    }

    private fun repo(dao: FakeComfyUIConnectionDao, client: HttpClient) =
        ComfyUIQueueRepositoryImpl(ComfyUIApiProvider(dao, client, testJson))

    private fun repo(dao: FakeComfyUIConnectionDao, body: String) = repo(dao, mockClient { okJson(body) })

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
        val repo = repo(daoWithActive(), body)

        val jobs = firstEmission(repo.observeQueue(intervalMs = 1000))

        assertEquals(3, jobs.size)
        assertEquals("run-1", jobs[0].promptId)
        assertEquals(QueueJobStatus.Running, jobs[0].status)
        assertEquals(QueueJobStatus.Queued, jobs[1].status)
        assertEquals("pend-2", jobs[2].promptId)
    }

    @Test
    fun observeQueue_emits_empty_when_no_active_connection() = runTest {
        // No active connection -> forActive() throws -> caught -> empty list emitted.
        val repo = repo(FakeComfyUIConnectionDao(), "{}")

        val jobs = firstEmission(repo.observeQueue(intervalMs = 1000))

        assertTrue(jobs.isEmpty())
    }

    @Test
    fun observeQueue_emits_empty_on_api_error() = runTest {
        val repo = repo(daoWithActive(), mockClient { respondError(HttpStatusCode.InternalServerError) })

        val jobs = firstEmission(repo.observeQueue(intervalMs = 1000))

        assertTrue(jobs.isEmpty())
    }

    private data class SentRequest(val method: HttpMethod, val path: String, val body: String)

    /** Answers GET /queue with [queueBody] and records every request with its body. */
    private fun recordingClient(queueBody: String, sent: MutableList<SentRequest>) = mockClient { req ->
        sent.add(SentRequest(req.method, req.url.encodedPath, req.body.toByteArray().decodeToString()))
        okJson(if (req.method == HttpMethod.Get && req.url.encodedPath == "/queue") queueBody else "{}")
    }

    private fun List<SentRequest>.posts(path: String) = filter { it.method == HttpMethod.Post && it.path == path }

    private fun json(text: String) = testJson.parseToJsonElement(text)

    @Test
    fun cancelJob_posts_delete_request_for_prompt_id() = runTest {
        val sent = mutableListOf<SentRequest>()
        val repo = repo(daoWithActive(), recordingClient("{}", sent))

        repo.cancelJob("abc")

        val deletes = sent.posts("/queue")
        assertEquals(1, deletes.size)
        assertEquals(json("""{"delete":["abc"]}"""), json(deletes.single().body))
    }

    @Test
    fun cancelJob_interrupts_only_the_running_prompt_when_it_is_running() = runTest {
        val sent = mutableListOf<SentRequest>()
        val queue = """{"queue_running":[[0,"abc"]],"queue_pending":[[1,"other"]]}"""
        val repo = repo(daoWithActive(), recordingClient(queue, sent))

        repo.cancelJob("abc")

        val interrupts = sent.posts("/interrupt")
        assertEquals(1, interrupts.size)
        assertEquals(json("""{"prompt_id":"abc"}"""), json(interrupts.single().body))
        assertTrue(sent.posts("/queue").isEmpty())
    }

    @Test
    fun cancelJob_deletes_a_pending_prompt_without_interrupting() = runTest {
        val sent = mutableListOf<SentRequest>()
        val queue = """{"queue_running":[[0,"other"]],"queue_pending":[[1,"abc"]]}"""
        val repo = repo(daoWithActive(), recordingClient(queue, sent))

        repo.cancelJob("abc")

        val deletes = sent.posts("/queue")
        assertEquals(1, deletes.size)
        assertEquals(json("""{"delete":["abc"]}"""), json(deletes.single().body))
        assertTrue(sent.posts("/interrupt").isEmpty())
    }

    @Test
    fun observeQueue_polls_the_connection_active_at_each_poll() = runTest {
        val dao = daoWithActive().apply {
            rows.add(ComfyUIConnectionEntity(id = 2, name = "B", hostname = "b", port = 9000, createdAt = 2))
        }
        val hosts = mutableListOf<String>()
        val client = mockClient {
            hosts.add("${it.url.host}:${it.url.port}")
            okJson("{}")
        }
        val repo = repo(dao, client)

        firstEmission(repo.observeQueue(intervalMs = 1000))
        dao.deactivateAll()
        dao.activate(2)
        firstEmission(repo.observeQueue(intervalMs = 1000))

        assertEquals(listOf("h:8188", "b:9000"), hosts)
    }
}
