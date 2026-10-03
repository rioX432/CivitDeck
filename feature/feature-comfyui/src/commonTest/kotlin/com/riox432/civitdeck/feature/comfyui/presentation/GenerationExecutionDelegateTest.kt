package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.data.image.ImageSaver
import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.domain.service.AppLifecycleTracker
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarter
import com.riox432.civitdeck.domain.usecase.ObserveGenerationNotificationsEnabledUseCase
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
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
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers Stop in [GenerationExecutionDelegate] against real queue and generation repositories on
 * a MockEngine: it cancels only this screen's prompt, also when pressed before /prompt answers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GenerationExecutionDelegateTest {

    private data class SentRequest(val method: HttpMethod, val path: String, val body: String)

    /** Answers GET /queue with [queueBody] and /prompt with p1 once [promptAnswer] completes. */
    private class Server(
        val queueBody: String,
        val promptAnswer: CompletableDeferred<Unit> = CompletableDeferred(Unit),
    ) {
        val sent = mutableListOf<SentRequest>()

        fun posts(path: String) = sent.filter { it.method == HttpMethod.Post && it.path == path }
    }

    // The engine runs on the test scheduler so runCurrent() leaves the delegate parked in its
    // first poll delay instead of racing a real IO thread.
    private fun TestScope.client(server: Server) = HttpClient(MockEngine) {
        engine {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { req ->
                val path = req.url.encodedPath
                server.sent.add(SentRequest(req.method, path, req.body.toByteArray().decodeToString()))
                when {
                    path == "/prompt" -> {
                        server.promptAnswer.await()
                        okJson("""{"prompt_id":"p1"}""")
                    }
                    req.method == HttpMethod.Get && path == "/queue" -> okJson(server.queueBody)
                    else -> okJson("{}")
                }
            }
        }
        install(ContentNegotiation) { json(testJson) }
    }

    private object NoConnection : ComfyUIConnectionRepository {
        override fun observeConnections(): Flow<List<ComfyUIConnection>> = flowOf(emptyList())
        override fun observeActiveConnection(): Flow<ComfyUIConnection?> = flowOf(null)

        // Null sends the delegate down the polling path instead of the WebSocket.
        override suspend fun getActiveConnection(): ComfyUIConnection? = null
        override suspend fun saveConnection(connection: ComfyUIConnection): Long = 0L
        override suspend fun deleteConnection(id: Long) = Unit
        override suspend fun activateConnection(id: Long) = Unit
        override suspend fun testConnection(connection: ComfyUIConnection): Boolean = true
        override suspend fun updateTestResult(id: Long, success: Boolean) = Unit
    }

    private fun TestScope.delegate(
        server: Server,
        uiState: MutableStateFlow<GenerationUiState>,
    ): GenerationExecutionDelegate {
        val dao = FakeComfyUIConnectionDao().apply {
            rows.add(ComfyUIConnectionEntity(id = 1, name = "A", hostname = "h", port = 8188, isActive = true, createdAt = 1))
        }
        val apiProvider = ComfyUIApiProvider(dao, client(server), testJson)
        val generationRepo = ComfyUIGenerationRepositoryImpl(apiProvider, testJson)
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
            repository = NoConnection,
            notificationService = FakeGenerationNotificationService(),
            lifecycleTracker = object : AppLifecycleTracker {
                override val isInForeground: Boolean = true
            },
            backgroundMonitorStarter = object : BackgroundMonitorStarter {
                override fun startMonitoring(promptId: String, baseUrl: String, wsScheme: String) = Unit
                override fun stopMonitoring() = Unit
            },
        )
        return GenerationExecutionDelegate(backgroundScope, uiState, useCases)
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

    @Test
    fun stop_interrupts_only_our_running_prompt_and_returns_to_idle() = runTest {
        val server = Server(queueBody = """{"queue_running":[[0,"p1"]],"queue_pending":[[1,"other"]]}""")
        val uiState = MutableStateFlow(GenerationUiState())
        val delegate = delegate(server, uiState)

        delegate.onGenerate(params)
        runCurrent()
        assertEquals(GenerationStatus.Running, uiState.value.generationStatus)

        delegate.onInterrupt()
        passSeveralPollIntervals()

        val interrupts = server.posts("/interrupt")
        assertEquals(1, interrupts.size)
        assertEquals(json("""{"prompt_id":"p1"}"""), json(interrupts.single().body))
        assertEquals(GenerationStatus.Idle, uiState.value.generationStatus)
        assertTrue(server.sent.none { it.path.startsWith("/history") })
    }

    @Test
    fun stop_before_prompt_answers_cancels_the_prompt_once_its_id_arrives() = runTest {
        val answer = CompletableDeferred<Unit>()
        val server = Server(
            queueBody = """{"queue_running":[[0,"other"]],"queue_pending":[[1,"p1"]]}""",
            promptAnswer = answer,
        )
        val uiState = MutableStateFlow(GenerationUiState())
        val delegate = delegate(server, uiState)

        delegate.onGenerate(params)
        runCurrent()
        assertEquals(GenerationStatus.Submitting, uiState.value.generationStatus)

        delegate.onInterrupt()
        runCurrent()
        assertEquals(GenerationStatus.Idle, uiState.value.generationStatus)
        assertTrue(server.posts("/queue").isEmpty())

        answer.complete(Unit)
        passSeveralPollIntervals()

        assertEquals(GenerationStatus.Idle, uiState.value.generationStatus)
        val deletes = server.posts("/queue")
        assertEquals(1, deletes.size)
        assertEquals(json("""{"delete":["p1"]}"""), json(deletes.single().body))
        assertTrue(server.posts("/interrupt").isEmpty())
        assertTrue(server.sent.none { it.path.startsWith("/history") })
    }

    private companion object {
        const val SEVERAL_POLL_INTERVALS_MS = 10_000L
    }
}
