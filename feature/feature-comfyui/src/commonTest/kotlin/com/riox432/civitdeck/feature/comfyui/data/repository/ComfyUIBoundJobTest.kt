package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.ComfyUIJobHandle
import com.riox432.civitdeck.domain.model.ComfyUIJobTarget
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Bound job operations on [ComfyUIGenerationRepositoryImpl] and [ComfyUIQueueRepositoryImpl]:
 * a job captured on server A keeps every request on A after B becomes active, and a deleted or
 * edited A fails instead of reaching B.
 */
class ComfyUIBoundJobTest {

    private data class Sent(val method: HttpMethod, val hostPort: String, val path: String, val body: String)

    private val sent = mutableListOf<Sent>()
    private val pinnedSent = mutableListOf<Sent>()
    private val createdPins = mutableListOf<String?>()

    private var queueBody = """{"queue_running":[],"queue_pending":[]}"""

    private fun recordingClient(log: MutableList<Sent>) = HttpClient(
        MockEngine { req ->
            log += Sent(req.method, "${req.url.host}:${req.url.port}", req.url.encodedPath, req.body.toByteArray().decodeToString())
            when (req.url.encodedPath) {
                "/prompt" -> okJson("""{"prompt_id":"p1"}""")
                "/history/p1" -> okJson(HISTORY_DONE)
                "/queue" -> okJson(if (req.method == HttpMethod.Get) queueBody else "{}")
                else -> okJson("{}")
            }
        },
    ) {
        install(ContentNegotiation) { json(testJson) }
        install(WebSockets)
    }

    private val rowA = ComfyUIConnectionEntity(id = 1, name = "A", hostname = "a", port = 8188, isActive = true, createdAt = 1)
    private val rowB = ComfyUIConnectionEntity(id = 2, name = "B", hostname = "b", port = 9000, createdAt = 2)

    private val dao = FakeComfyUIConnectionDao()

    private val provider by lazy {
        ComfyUIApiProvider(dao, recordingClient(sent), testJson) { trust ->
            createdPins += trust.expectedSha256
            recordingClient(pinnedSent)
        }
    }
    private val generation by lazy { ComfyUIGenerationRepositoryImpl(provider, testJson) }
    private val queue by lazy { ComfyUIQueueRepositoryImpl(provider) }

    private val params = ComfyUIGenerationParams(checkpoint = "m", prompt = "p")

    private fun ComfyUIConnectionEntity.target() =
        ComfyUIJobTarget(id, hostname, port, useHttps, acceptSelfSigned, tlsCertSha256)

    /** Stores [a] and B with A active, captures A, then makes B the active connection. */
    private suspend fun captureAThenActivateB(a: ComfyUIConnectionEntity = rowA): ComfyUIJobTarget {
        dao.rows += a
        dao.rows += rowB
        val target = a.target()
        dao.deactivateAll()
        dao.activate(rowB.id)
        return target
    }

    private fun hosts(log: List<Sent> = sent) = log.map { it.hostPort }.distinct()

    @Test
    fun submit_poll_socket_and_result_urls_stay_on_the_captured_server() = runTest {
        val target = captureAThenActivateB()

        val job = generation.submitGeneration(params, target)
        val result = generation.pollGenerationResult(job)
        // The mock cannot complete a WebSocket handshake; only the handshake host matters here.
        runCatching { generation.observeGenerationProgress(job).toList() }

        assertEquals(ComfyUIJobHandle(target, "p1"), job)
        assertEquals(GenerationStatus.Completed, result.status)
        assertTrue(result.imageUrls.single().startsWith("http://a:8188/view"), result.imageUrls.toString())
        assertTrue(sent.any { it.path == "/ws" }, "socket handshake was not attempted")
        assertEquals(listOf("a:8188"), hosts())
    }

    @Test
    fun cancel_interrupts_the_running_prompt_on_the_captured_server_only() = runTest {
        val target = captureAThenActivateB()
        queueBody = """{"queue_running":[[0,"p1"]],"queue_pending":[]}"""

        queue.cancelJob(ComfyUIJobHandle(target, "p1"))

        val interrupt = sent.single { it.method == HttpMethod.Post && it.path == "/interrupt" }
        assertEquals(testJson.parseToJsonElement("""{"prompt_id":"p1"}"""), testJson.parseToJsonElement(interrupt.body))
        assertEquals(listOf("a:8188"), hosts())
    }

    @Test
    fun cancel_deletes_the_queued_prompt_on_the_captured_server_only() = runTest {
        val target = captureAThenActivateB()
        queueBody = """{"queue_running":[[0,"other"]],"queue_pending":[[1,"p1"]]}"""

        queue.cancelJob(ComfyUIJobHandle(target, "p1"))

        val delete = sent.single { it.method == HttpMethod.Post && it.path == "/queue" }
        assertEquals(testJson.parseToJsonElement("""{"delete":["p1"]}"""), testJson.parseToJsonElement(delete.body))
        assertTrue(sent.none { it.path == "/interrupt" })
        assertEquals(listOf("a:8188"), hosts())
    }

    @Test
    fun a_handle_for_a_never_touches_the_same_prompt_id_on_b() = runTest {
        val target = captureAThenActivateB()
        val job = ComfyUIJobHandle(target, "p1")

        generation.pollGenerationResult(job)
        queue.cancelJob(job)

        assertTrue(sent.none { it.hostPort == "b:9000" }, sent.toString())
    }

    @Test
    fun deleted_target_fails_every_operation_without_reaching_the_active_server() = runTest {
        val target = captureAThenActivateB()
        dao.deleteById(rowA.id)
        val job = ComfyUIJobHandle(target, "p1")

        assertFailsWith<DomainException.ConnectionException> { generation.submitGeneration(params, target) }
        assertFailsWith<DomainException.ConnectionException> { generation.pollGenerationResult(job) }
        assertFailsWith<DomainException.ConnectionException> { generation.observeGenerationProgress(job).toList() }
        assertFailsWith<DomainException.ConnectionException> { queue.cancelJob(job) }

        assertTrue(sent.isEmpty(), sent.toString())
    }

    @Test
    fun edited_target_endpoint_or_tls_fails_without_reaching_any_server() = runTest {
        val target = captureAThenActivateB()
        val job = ComfyUIJobHandle(target, "p1")

        listOf(
            rowA.copy(hostname = "a2"),
            rowA.copy(port = 8189),
            rowA.copy(useHttps = true),
            rowA.copy(acceptSelfSigned = true),
            rowA.copy(tlsCertSha256 = PIN),
        ).forEach { edited ->
            dao.update(edited)
            assertFailsWith<DomainException.ConnectionException>("edited: $edited") { generation.pollGenerationResult(job) }
            assertFailsWith<DomainException.ConnectionException>("edited: $edited") { queue.cancelJob(job) }
        }

        assertTrue(sent.isEmpty(), sent.toString())
    }

    @Test
    fun renaming_the_captured_connection_keeps_the_job_reachable() = runTest {
        val target = captureAThenActivateB()
        dao.update(dao.rows.first { it.id == rowA.id }.copy(name = "Renamed", lastTestedAt = 5, lastTestSuccess = false))

        generation.pollGenerationResult(ComfyUIJobHandle(target, "p1"))

        assertEquals(listOf("a:8188"), hosts())
    }

    @Test
    fun pinned_target_uses_its_pinned_client_on_the_captured_url() = runTest {
        val pinnedA = rowA.copy(port = 8443, useHttps = true, acceptSelfSigned = true, tlsCertSha256 = PIN)
        val target = captureAThenActivateB(pinnedA)

        val result = generation.pollGenerationResult(ComfyUIJobHandle(target, "p1"))

        assertEquals(listOf<String?>(PIN), createdPins)
        assertEquals(listOf("a:8443"), hosts(pinnedSent))
        assertTrue(sent.isEmpty(), sent.toString())
        assertTrue(result.imageUrls.single().startsWith("https://a:8443/view"), result.imageUrls.toString())
    }

    private companion object {
        val PIN = "ab".repeat(32)
        const val HISTORY_DONE =
            """{"p1":{"status":{"status_str":"success"},"outputs":{"9":{"images":[{"filename":"o.png","type":"output"}]}}}}"""
    }
}
