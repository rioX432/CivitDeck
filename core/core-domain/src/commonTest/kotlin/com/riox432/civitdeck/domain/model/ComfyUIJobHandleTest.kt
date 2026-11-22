package com.riox432.civitdeck.domain.model

import com.riox432.civitdeck.domain.repository.ComfyUIGenerationRepository
import com.riox432.civitdeck.domain.repository.ComfyUIQueueRepository
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ComfyUIJobHandleTest {

    private val connectionA = ComfyUIConnection(
        id = 1L,
        name = "Desk PC",
        hostname = "192.168.1.10",
        port = 8188,
        isActive = true,
    )

    @Test
    fun target_ignores_selection_name_and_test_metadata() {
        val target = ComfyUIJobTarget.from(connectionA)

        val renamedAndDeselected = connectionA.copy(
            name = "Renamed",
            isActive = false,
            lastTestedAt = 123L,
            lastTestSuccess = false,
        )

        assertEquals(target, ComfyUIJobTarget.from(renamedAndDeselected))
    }

    @Test
    fun target_changes_with_endpoint_or_tls_settings() {
        val target = ComfyUIJobTarget.from(connectionA)

        listOf(
            connectionA.copy(hostname = "192.168.1.11"),
            connectionA.copy(port = 8189),
            connectionA.copy(useHttps = true),
            connectionA.copy(acceptSelfSigned = true),
            connectionA.copy(tlsCertSha256 = "ab".repeat(32)),
            connectionA.copy(id = 2L),
        ).forEach { changed ->
            assertNotEquals(target, ComfyUIJobTarget.from(changed), "changed: $changed")
        }
    }

    @Test
    fun target_derives_urls_from_its_own_transport() {
        val http = ComfyUIJobTarget.from(connectionA)
        val https = ComfyUIJobTarget.from(connectionA.copy(useHttps = true, port = 443))

        assertEquals("http://192.168.1.10:8188", http.baseUrl)
        assertEquals("ws", http.wsScheme)
        assertEquals("https://192.168.1.10:443", https.baseUrl)
        assertEquals("wss", https.wsScheme)
    }

    @Test
    fun same_prompt_id_on_two_targets_is_two_jobs() {
        val onA = ComfyUIJobHandle(ComfyUIJobTarget.from(connectionA), "prompt-1")
        val onB = ComfyUIJobHandle(
            ComfyUIJobTarget.from(connectionA.copy(id = 2L, hostname = "192.168.1.20")),
            "prompt-1",
        )

        assertNotEquals(onA, onB)
    }

    @Test
    fun unimplemented_bound_generation_operations_fail_without_calling_legacy_methods() = runTest {
        val repository = LegacyOnlyGenerationRepository()
        val job = ComfyUIJobHandle(ComfyUIJobTarget.from(connectionA), "prompt-1")

        assertFailsWith<UnsupportedOperationException> {
            repository.submitGeneration(ComfyUIGenerationParams(checkpoint = "c", prompt = "p"), job.target)
        }
        assertFailsWith<UnsupportedOperationException> { repository.pollGenerationResult(job) }
        assertFailsWith<UnsupportedOperationException> { repository.observeGenerationProgress(job) }
        val params = ComfyUIGenerationParams(checkpoint = "c", prompt = "p", seed = 1)
        assertFailsWith<UnsupportedOperationException> {
            repository.prepareReferenceGeneration(referenceId = 1, target = job.target, params = params)
        }
        val prepared = PreparedReferenceGeneration(1, job.target, params, "{}", EffectiveGenerationSettings.from(params))
        assertFailsWith<UnsupportedOperationException> { repository.submitPreparedReference(prepared) }

        assertTrue(repository.legacyCalls.isEmpty(), "legacy calls: ${repository.legacyCalls}")
    }

    @Test
    fun unimplemented_bound_cancel_fails_without_cancelling_by_prompt_id() = runTest {
        val repository = LegacyOnlyQueueRepository()
        val job = ComfyUIJobHandle(ComfyUIJobTarget.from(connectionA), "prompt-1")

        assertFailsWith<UnsupportedOperationException> { repository.cancelJob(job) }

        assertTrue(repository.cancelledPromptIds.isEmpty())
    }

    @Test
    fun background_monitor_default_hands_off_the_captured_target() {
        val starter = RecordingMonitorStarter()
        val target = ComfyUIJobTarget.from(connectionA.copy(useHttps = true, port = 8443))

        starter.startMonitoring(ComfyUIJobHandle(target, "prompt-1"))

        assertEquals(listOf(Triple("prompt-1", "https://192.168.1.10:8443", "wss")), starter.started)
    }

    private class LegacyOnlyGenerationRepository : ComfyUIGenerationRepository {
        val legacyCalls = mutableListOf<String>()

        override suspend fun fetchCheckpoints(): List<String> = emptyList()
        override suspend fun fetchLoras(): List<String> = emptyList()
        override suspend fun fetchControlNets(): List<String> = emptyList()
        override suspend fun fetchDiffusionModelResources() = DiffusionModelResources()

        override suspend fun submitGeneration(params: ComfyUIGenerationParams): String {
            legacyCalls += "submit"
            return "legacy"
        }

        override suspend fun pollGenerationResult(promptId: String): GenerationResult {
            legacyCalls += "poll"
            return GenerationResult(promptId, GenerationStatus.Running)
        }

        override fun observeGenerationProgress(promptId: String, host: String, port: Int): Flow<GenerationProgress> {
            legacyCalls += "progress(host)"
            return emptyFlow()
        }

        override fun observeGenerationProgress(
            promptId: String,
            baseUrl: String,
            wsScheme: String,
        ): Flow<GenerationProgress> {
            legacyCalls += "progress(baseUrl)"
            return emptyFlow()
        }

        override fun getImageUrl(filename: String, subfolder: String, type: String): String = ""

        override suspend fun interruptGeneration() {
            legacyCalls += "interrupt"
        }

        override suspend fun uploadMaskImage(maskPngBytes: ByteArray): String = ""
        override suspend fun fetchObjectInfo(): String = "{}"
    }

    private class LegacyOnlyQueueRepository : ComfyUIQueueRepository {
        val cancelledPromptIds = mutableListOf<String>()

        override fun observeQueue(intervalMs: Long): Flow<List<QueueJob>> = emptyFlow()

        override suspend fun cancelJob(promptId: String) {
            cancelledPromptIds += promptId
        }
    }

    private class RecordingMonitorStarter : BackgroundMonitorStarter {
        val started = mutableListOf<Triple<String, String, String>>()

        override fun startMonitoring(promptId: String, baseUrl: String, wsScheme: String) {
            started += Triple(promptId, baseUrl, wsScheme)
        }

        override fun stopMonitoring() = Unit
    }
}
