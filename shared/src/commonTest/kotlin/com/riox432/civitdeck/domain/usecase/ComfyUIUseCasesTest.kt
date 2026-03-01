package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationProgress
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.QueueJob
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.domain.repository.ComfyUIGenerationRepository
import com.riox432.civitdeck.domain.repository.ComfyUIQueueRepository
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DeleteComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUICheckpointsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveComfyUIConnectionsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PollComfyUIResultUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SubmitComfyUIGenerationUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.TestComfyUIConnectionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComfyUIUseCasesTest {

    private class FakeComfyUIRepository : ComfyUIConnectionRepository, ComfyUIGenerationRepository, ComfyUIQueueRepository {
        val connections = MutableStateFlow(listOf<ComfyUIConnection>())
        var savedConnection: ComfyUIConnection? = null
        var deletedId: Long? = null
        var activatedId: Long? = null
        var testResult = true
        var testResultUpdated = false
        var fetchedCheckpoints = listOf("model_a.safetensors", "model_b.safetensors")
        var submittedParams: ComfyUIGenerationParams? = null
        var pollResult = GenerationResult("test-id", GenerationStatus.Completed, listOf("http://img.png"))

        override fun observeConnections(): Flow<List<ComfyUIConnection>> = connections
        override fun observeActiveConnection(): Flow<ComfyUIConnection?> =
            connections.map { list -> list.firstOrNull { it.isActive } }

        override suspend fun getActiveConnection(): ComfyUIConnection? =
            connections.value.firstOrNull { it.isActive }

        override suspend fun saveConnection(connection: ComfyUIConnection): Long {
            savedConnection = connection
            return 1L
        }

        override suspend fun deleteConnection(id: Long) { deletedId = id }
        override suspend fun activateConnection(id: Long) { activatedId = id }
        override suspend fun testConnection(connection: ComfyUIConnection): Boolean = testResult
        override suspend fun updateTestResult(id: Long, success: Boolean) { testResultUpdated = true }
        override suspend fun fetchCheckpoints(): List<String> = fetchedCheckpoints
        override suspend fun fetchLoras(): List<String> = emptyList()
        override suspend fun fetchControlNets(): List<String> = emptyList()
        override suspend fun submitGeneration(params: ComfyUIGenerationParams): String {
            submittedParams = params
            return "prompt-123"
        }

        override suspend fun pollGenerationResult(promptId: String): GenerationResult = pollResult
        override fun observeGenerationProgress(promptId: String, host: String, port: Int): Flow<GenerationProgress> =
            kotlinx.coroutines.flow.emptyFlow()
        override fun getImageUrl(filename: String, subfolder: String, type: String): String =
            "http://localhost:8188/view?filename=$filename"
        override fun observeQueue(intervalMs: Long): Flow<List<QueueJob>> =
            kotlinx.coroutines.flow.flowOf(emptyList())
        override suspend fun cancelJob(promptId: String) {}
    }

    private val repo = FakeComfyUIRepository()

    @Test
    fun observeConnections_emits_list() = runTest {
        val conn = ComfyUIConnection(id = 1, name = "Test", hostname = "192.168.1.1")
        repo.connections.value = listOf(conn)
        val useCase = ObserveComfyUIConnectionsUseCase(repo)
        val result = useCase().first()
        assertEquals(1, result.size)
        assertEquals("Test", result[0].name)
    }

    @Test
    fun saveConnection_delegates_to_repo() = runTest {
        val conn = ComfyUIConnection(name = "Home", hostname = "10.0.0.1", port = 8188)
        val useCase = SaveComfyUIConnectionUseCase(repo)
        val id = useCase(conn)
        assertEquals(1L, id)
        assertEquals("Home", repo.savedConnection?.name)
    }

    @Test
    fun deleteConnection_delegates_to_repo() = runTest {
        val useCase = DeleteComfyUIConnectionUseCase(repo)
        useCase(42L)
        assertEquals(42L, repo.deletedId)
    }

    @Test
    fun testConnection_success_updates_result() = runTest {
        repo.testResult = true
        val conn = ComfyUIConnection(id = 1, name = "Test", hostname = "localhost")
        val useCase = TestComfyUIConnectionUseCase(repo)
        val success = useCase(conn)
        assertTrue(success)
        assertTrue(repo.testResultUpdated)
    }

    @Test
    fun fetchCheckpoints_returns_list() = runTest {
        val useCase = FetchComfyUICheckpointsUseCase(repo)
        val result = useCase()
        assertEquals(2, result.size)
        assertEquals("model_a.safetensors", result[0])
    }

    @Test
    fun submitGeneration_returns_prompt_id() = runTest {
        val params = ComfyUIGenerationParams(
            checkpoint = "model.safetensors",
            prompt = "a cat",
        )
        val useCase = SubmitComfyUIGenerationUseCase(repo)
        val promptId = useCase(params)
        assertEquals("prompt-123", promptId)
        assertEquals("a cat", repo.submittedParams?.prompt)
    }

    @Test
    fun pollResult_returns_completed() = runTest {
        val useCase = PollComfyUIResultUseCase(repo)
        val result = useCase("prompt-123")
        assertEquals(GenerationStatus.Completed, result.status)
        assertEquals(1, result.imageUrls.size)
    }
}
