package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.data.image.ImageSaver
import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.ComfyUIJobTarget
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.domain.service.AppLifecycleTracker
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarter
import com.riox432.civitdeck.domain.usecase.ObserveGenerationNotificationsEnabledUseCase
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import com.riox432.civitdeck.feature.comfyui.data.repository.ComfyUIConnectionRepositoryImpl
import com.riox432.civitdeck.feature.comfyui.data.repository.ComfyUIGenerationRepositoryImpl
import com.riox432.civitdeck.feature.comfyui.data.repository.ComfyUIQueueRepositoryImpl
import com.riox432.civitdeck.feature.comfyui.data.repository.FakeComfyUIConnectionDao
import com.riox432.civitdeck.feature.comfyui.data.repository.okJson
import com.riox432.civitdeck.feature.comfyui.data.repository.testJson
import com.riox432.civitdeck.feature.comfyui.domain.usecase.CancelComfyUIJobUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveGenerationProgressUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PollComfyUIResultUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SubmitComfyUIGenerationUseCase
import com.riox432.civitdeck.testing.FakeAppBehaviorPreferencesRepository
import com.riox432.civitdeck.testing.FakeGenerationNotificationService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [GenerationExecutionDelegate] against real connection, queue and generation repositories
 * on a MockEngine: a job stays on the server captured at submission when another server is
 * activated, Stop cancels only this screen's prompt (also before /prompt answers), and an older
 * submission or cancel never takes over a newer generation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GenerationExecutionDelegateTest {

    private data class SentRequest(val method: HttpMethod, val hostPort: String, val path: String, val body: String)

    /**
     * The n-th /prompt answers `p<n>` once its gate in [promptGates] completes (immediately when
     * there is none), or 500 when n is in [failingPrompts].
     */
    private class Server {
        val sent = mutableListOf<SentRequest>()
        val promptGates = mutableMapOf<Int, CompletableDeferred<Unit>>()
        val failingPrompts = mutableSetOf<Int>()
        var queueBody = EMPTY_QUEUE
        var historyBody = "{}"
        var interruptGate = CompletableDeferred(Unit)
        private var promptCount = 0

        fun posts(path: String) = sent.filter { it.method == HttpMethod.Post && it.path == path }

        fun hosts() = sent.map { it.hostPort }.distinct()

        fun nextPrompt(): Int = ++promptCount
    }

    private class RecordingMonitor : BackgroundMonitorStarter {
        val started = mutableListOf<Triple<String, String, String>>()
        override fun startMonitoring(promptId: String, baseUrl: String, wsScheme: String) {
            started += Triple(promptId, baseUrl, wsScheme)
        }
        override fun stopMonitoring() = Unit
    }

    /** Counts reads of the active connection; the delegate may read it once per Generate. */
    private class CountingConnections(
        private val inner: ComfyUIConnectionRepository,
    ) : ComfyUIConnectionRepository by inner {
        var activeReads = 0
        override suspend fun getActiveConnection(): ComfyUIConnection? {
            activeReads++
            return inner.getActiveConnection()
        }
    }

    private val server = Server()
    private val monitor = RecordingMonitor()
    private val dao = FakeComfyUIConnectionDao()
    private val uiState = MutableStateFlow(GenerationUiState())
    private lateinit var connections: CountingConnections

    private val rowA = ComfyUIConnectionEntity(id = 1, name = "A", hostname = "a", port = 8188, createdAt = 1)
    private val rowB = ComfyUIConnectionEntity(id = 2, name = "B", hostname = "b", port = 9000, createdAt = 2)

    private fun ComfyUIConnectionEntity.target() =
        ComfyUIJobTarget(id, hostname, port, useHttps, acceptSelfSigned, tlsCertSha256)

    // The engine runs on the test scheduler so runCurrent() leaves the delegate parked in its
    // first poll delay instead of racing a real IO thread. WebSockets is installed so the
    // progress socket's handshake request is recorded; the mock cannot complete it, so the
    // delegate falls back to polling.
    private fun TestScope.client() = HttpClient(MockEngine) {
        engine {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { req ->
                val path = req.url.encodedPath
                server.sent.add(
                    SentRequest(req.method, "${req.url.host}:${req.url.port}", path, req.body.toByteArray().decodeToString()),
                )
                when {
                    path == "/prompt" -> {
                        val n = server.nextPrompt()
                        server.promptGates[n]?.await()
                        if (n in server.failingPrompts) {
                            respondError(HttpStatusCode.InternalServerError)
                        } else {
                            okJson("""{"prompt_id":"p$n"}""")
                        }
                    }
                    req.method == HttpMethod.Get && path == "/queue" -> okJson(server.queueBody)
                    path == "/interrupt" -> {
                        server.interruptGate.await()
                        okJson("{}")
                    }
                    path.startsWith("/history/") -> okJson(server.historyBody)
                    else -> okJson("{}")
                }
            }
        }
        install(ContentNegotiation) { json(testJson) }
        install(WebSockets)
    }

    /** Stores [rows], activates [active], and builds the delegate on them. */
    private fun TestScope.delegate(
        vararg rows: ComfyUIConnectionEntity = arrayOf(rowA, rowB),
        active: Long? = rowA.id,
    ): GenerationExecutionDelegate {
        dao.rows.addAll(rows.map { it.copy(isActive = it.id == active) })
        val apiProvider = ComfyUIApiProvider(dao, client(), testJson)
        val generationRepo = ComfyUIGenerationRepositoryImpl(apiProvider, testJson)
        connections = CountingConnections(ComfyUIConnectionRepositoryImpl(dao, apiProvider))
        val useCases = GenerationExecutionUseCases(
            submitGeneration = SubmitComfyUIGenerationUseCase(generationRepo),
            pollResult = PollComfyUIResultUseCase(generationRepo),
            observeProgress = ObserveGenerationProgressUseCase(generationRepo),
            cancelJob = CancelComfyUIJobUseCase(ComfyUIQueueRepositoryImpl(apiProvider)),
            saveImage = SaveGeneratedImageUseCase(
                apiProvider,
                object : ImageSaver {
                    override suspend fun saveToGallery(imageBytes: ByteArray, filename: String) = false
                },
            ),
            observeGenNotifEnabled = ObserveGenerationNotificationsEnabledUseCase(
                FakeAppBehaviorPreferencesRepository(),
            ),
            repository = connections,
            notificationService = FakeGenerationNotificationService(),
            lifecycleTracker = object : AppLifecycleTracker {
                override val isInForeground: Boolean = true
            },
            backgroundMonitorStarter = monitor,
        )
        return GenerationExecutionDelegate(backgroundScope, uiState, useCases)
    }

    private suspend fun activateB() {
        dao.deactivateAll()
        dao.activate(rowB.id)
    }

    private fun json(text: String) = testJson.parseToJsonElement(text)

    private val params = ComfyUIGenerationParams(checkpoint = "a.safetensors", prompt = "p")

    /**
     * Runs past several poll intervals so a poll that was not stopped would show up.
     * advanceUntilIdle() is not used because it skips work in backgroundScope, where the
     * delegate runs.
     */
    private fun TestScope.passSeveralPollIntervals() {
        advanceTimeBy(SEVERAL_POLL_INTERVALS_MS)
        runCurrent()
    }

    private fun TestScope.gatePrompt(n: Int) = CompletableDeferred<Unit>().also { server.promptGates[n] = it }

    @Test
    fun job_submitted_to_a_keeps_socket_polling_and_results_on_a_after_b_is_activated() = runTest {
        val delegate = delegate()
        val answer = gatePrompt(1)
        server.historyBody = HISTORY_P1_DONE

        delegate.onGenerate(params)
        runCurrent()
        activateB()
        answer.complete(Unit)
        passSeveralPollIntervals()

        assertEquals(GenerationStatus.Completed, uiState.value.generationStatus)
        val imageUrl = uiState.value.result?.imageUrls?.single().orEmpty()
        assertTrue(imageUrl.startsWith("http://a:8188/view"), imageUrl)
        assertTrue(server.sent.any { it.path == "/ws" }, "progress socket was not attempted")
        assertTrue(server.sent.any { it.path == "/history/p1" }, "fallback polling did not run")
        assertEquals(listOf("a:8188"), server.hosts())
        assertEquals(listOf(Triple("p1", "http://a:8188", "ws")), monitor.started)
        assertEquals(1, connections.activeReads)
    }

    @Test
    fun stop_interrupts_only_our_running_prompt_on_its_server_and_returns_to_idle() = runTest {
        val delegate = delegate()
        server.queueBody = """{"queue_running":[[0,"p1"]],"queue_pending":[[1,"other"]]}"""

        delegate.onGenerate(params)
        runCurrent()
        assertEquals(GenerationStatus.Running, uiState.value.generationStatus)

        activateB()
        delegate.onInterrupt()
        passSeveralPollIntervals()

        val interrupts = server.posts("/interrupt")
        assertEquals(1, interrupts.size)
        assertEquals(json("""{"prompt_id":"p1"}"""), json(interrupts.single().body))
        assertEquals(GenerationStatus.Idle, uiState.value.generationStatus)
        assertTrue(server.sent.none { it.path.startsWith("/history") })
        assertEquals(listOf("a:8188"), server.hosts())
    }

    @Test
    fun stop_before_prompt_answers_cancels_the_prompt_on_its_server_once_its_id_arrives() = runTest {
        val delegate = delegate()
        val answer = gatePrompt(1)
        server.queueBody = """{"queue_running":[[0,"other"]],"queue_pending":[[1,"p1"]]}"""

        delegate.onGenerate(params)
        runCurrent()
        assertEquals(GenerationStatus.Submitting, uiState.value.generationStatus)

        delegate.onInterrupt()
        runCurrent()
        assertEquals(GenerationStatus.Idle, uiState.value.generationStatus)
        assertTrue(server.posts("/queue").isEmpty())

        activateB()
        answer.complete(Unit)
        passSeveralPollIntervals()

        assertEquals(GenerationStatus.Idle, uiState.value.generationStatus)
        val deletes = server.posts("/queue")
        assertEquals(1, deletes.size)
        assertEquals(json("""{"delete":["p1"]}"""), json(deletes.single().body))
        assertTrue(server.posts("/interrupt").isEmpty())
        assertTrue(server.sent.none { it.path.startsWith("/history") })
        assertEquals(listOf("a:8188"), server.hosts())
    }

    @Test
    fun older_submission_answering_late_does_not_take_over_the_newer_generation() = runTest {
        val delegate = delegate()
        val firstAnswer = gatePrompt(1)

        delegate.onGenerate(params)
        runCurrent()
        delegate.onGenerate(params)
        runCurrent()
        assertEquals(GenerationStatus.Running, uiState.value.generationStatus)

        firstAnswer.complete(Unit)
        passSeveralPollIntervals()

        assertEquals(GenerationStatus.Running, uiState.value.generationStatus)
        assertTrue(server.sent.any { it.path == "/history/p2" })
        assertTrue(server.sent.none { it.path == "/history/p1" }, server.sent.toString())
        assertEquals(listOf(Triple("p2", "http://a:8188", "ws")), monitor.started)
    }

    @Test
    fun older_submission_failing_late_does_not_overwrite_the_newer_generation() = runTest {
        val delegate = delegate()
        val firstAnswer = gatePrompt(1)
        server.failingPrompts += 1

        delegate.onGenerate(params)
        runCurrent()
        delegate.onGenerate(params)
        runCurrent()
        firstAnswer.complete(Unit)
        passSeveralPollIntervals()

        assertEquals(GenerationStatus.Running, uiState.value.generationStatus)
        assertNull(uiState.value.error)
    }

    @Test
    fun older_cancel_finishing_late_does_not_reset_the_newer_generation() = runTest {
        val delegate = delegate()
        server.queueBody = """{"queue_running":[[0,"p1"]],"queue_pending":[]}"""
        server.interruptGate = CompletableDeferred()

        delegate.onGenerate(params)
        runCurrent()
        delegate.onInterrupt()
        runCurrent()
        delegate.onGenerate(params)
        runCurrent()
        assertEquals(GenerationStatus.Running, uiState.value.generationStatus)

        server.interruptGate.complete(Unit)
        passSeveralPollIntervals()

        assertEquals(GenerationStatus.Running, uiState.value.generationStatus)
        assertEquals(json("""{"prompt_id":"p1"}"""), json(server.posts("/interrupt").single().body))
    }

    @Test
    fun explicit_target_submits_to_it_while_another_server_is_active() = runTest {
        val delegate = delegate(active = rowB.id)

        delegate.onGenerate(params, rowA.target())
        runCurrent()

        assertEquals(GenerationStatus.Running, uiState.value.generationStatus)
        assertEquals(1, server.posts("/prompt").size)
        assertEquals(listOf("a:8188"), server.hosts())
        assertEquals(0, connections.activeReads)
    }

    @Test
    fun explicit_target_edited_since_capture_fails_without_reaching_any_server() = runTest {
        val delegate = delegate(active = rowB.id)
        val captured = rowA.target()
        dao.update(rowA.copy(port = 8189))

        delegate.onGenerate(params, captured)
        runCurrent()

        assertEquals(GenerationStatus.Error, uiState.value.generationStatus)
        assertTrue(server.sent.isEmpty(), server.sent.toString())
    }

    @Test
    fun generate_without_an_active_connection_fails_without_a_request() = runTest {
        val delegate = delegate(active = null)

        delegate.onGenerate(params)
        runCurrent()

        assertEquals(GenerationStatus.Error, uiState.value.generationStatus)
        assertTrue(server.sent.isEmpty(), server.sent.toString())
    }

    @Test
    fun background_monitor_gets_the_https_target_of_the_handle_as_wss() = runTest {
        val httpsA = rowA.copy(port = 8443, useHttps = true)
        val delegate = delegate(httpsA, rowB)

        delegate.onGenerate(params)
        runCurrent()

        assertEquals(listOf(Triple("p1", "https://a:8443", "wss")), monitor.started)
        assertEquals(listOf("a:8443"), server.hosts())
    }

    private companion object {
        const val SEVERAL_POLL_INTERVALS_MS = 10_000L
        const val EMPTY_QUEUE = """{"queue_running":[],"queue_pending":[]}"""
        const val HISTORY_P1_DONE =
            """{"p1":{"status":{"status_str":"success"},"outputs":{"9":{"images":[{"filename":"o.png","type":"output"}]}}}}"""
    }
}
