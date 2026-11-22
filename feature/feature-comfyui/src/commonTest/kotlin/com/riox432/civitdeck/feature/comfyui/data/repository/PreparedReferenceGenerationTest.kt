package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.ComfyUIJobHandle
import com.riox432.civitdeck.domain.model.ComfyUIJobTarget
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.domain.model.LoraSelection
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * [ComfyUIGenerationRepositoryImpl.prepareReferenceGeneration] fixes the graph and seed once, and
 * [ComfyUIGenerationRepositoryImpl.submitPreparedReference] sends that graph unchanged to the
 * captured server only.
 */
class PreparedReferenceGenerationTest {

    private data class Sent(val hostPort: String, val path: String, val body: String)

    /** Returns [value] for every draw and counts the draws. */
    private class CountingRandom(private val value: Long) : Random() {
        var draws = 0
        override fun nextBits(bitCount: Int): Int = error("unexpected nextBits")
        override fun nextLong(from: Long, until: Long): Long {
            draws++
            return value
        }
    }

    private val sent = mutableListOf<Sent>()
    private val random = CountingRandom(RESOLVED_SEED)
    private val dao = FakeComfyUIConnectionDao()

    private val repository by lazy {
        val client = HttpClient(
            MockEngine { req ->
                sent += Sent("${req.url.host}:${req.url.port}", req.url.encodedPath, req.body.toByteArray().decodeToString())
                okJson("""{"prompt_id":"p1"}""")
            },
        ) {
            install(ContentNegotiation) { json(testJson) }
            install(WebSockets)
        }
        ComfyUIGenerationRepositoryImpl(ComfyUIApiProvider(dao, client, testJson), testJson, random)
    }

    private val rowA = ComfyUIConnectionEntity(id = 1, name = "A", hostname = "a", port = 8188, isActive = true, createdAt = 1)
    private val rowB = ComfyUIConnectionEntity(id = 2, name = "B", hostname = "b", port = 9000, createdAt = 2)

    private val params = ComfyUIGenerationParams(
        checkpoint = "sd15.safetensors",
        prompt = "a cat",
        negativePrompt = "blurry",
        seed = -1,
        loraSelections = listOf(LoraSelection("a.safetensors", 0.5f, 0.25f)),
    )

    private fun ComfyUIConnectionEntity.target() =
        ComfyUIJobTarget(id, hostname, port, useHttps, acceptSelfSigned, tlsCertSha256)

    private fun storedTargetA(): ComfyUIJobTarget {
        dao.rows += rowA
        dao.rows += rowB
        return rowA.target()
    }

    private fun JsonObject.ksamplerSeed(): Long = values
        .map { it.jsonObject }
        .single { it["class_type"]?.jsonPrimitive?.content == "KSampler" }
        .getValue("inputs").jsonObject.getValue("seed").jsonPrimitive.long

    private fun sentPromptGraph(): JsonObject =
        testJson.parseToJsonElement(sent.single { it.path == "/prompt" }.body).jsonObject.getValue("prompt").jsonObject

    @Test
    fun a_random_seed_is_drawn_once_and_the_stored_graph_is_sent_unchanged() = runTest {
        val target = storedTargetA()

        val prepared = repository.prepareReferenceGeneration(REFERENCE_ID, target, params)
        val job = repository.submitPreparedReference(prepared)

        assertEquals(1, random.draws)
        assertEquals(RESOLVED_SEED, prepared.effectiveParams.seed)
        assertEquals(RESOLVED_SEED, prepared.effectiveSettings.seed)
        assertEquals(RESOLVED_SEED, testJson.parseToJsonElement(prepared.workflowJson).jsonObject.ksamplerSeed())
        assertEquals(prepared.workflowJson, testJson.encodeToString(JsonObject.serializer(), sentPromptGraph()))
        assertEquals(ComfyUIJobHandle(target, "p1"), job)
        assertEquals(REFERENCE_ID, prepared.referenceId)
    }

    @Test
    fun a_fixed_seed_is_kept_without_drawing() = runTest {
        val prepared = repository.prepareReferenceGeneration(REFERENCE_ID, storedTargetA(), params.copy(seed = 42))
        repository.submitPreparedReference(prepared)

        assertEquals(0, random.draws)
        assertEquals(42L, prepared.effectiveSettings.seed)
        assertEquals(42L, sentPromptGraph().ksamplerSeed())
    }

    @Test
    fun later_changes_to_the_caller_lora_list_do_not_reach_the_prepared_settings() = runTest {
        val loras = mutableListOf(LoraSelection("a.safetensors", 0.5f, 0.25f))

        val prepared = repository.prepareReferenceGeneration(REFERENCE_ID, storedTargetA(), params.copy(loraSelections = loras))
        loras += LoraSelection("b.safetensors")

        assertEquals(listOf(LoraSelection("a.safetensors", 0.5f, 0.25f)), prepared.effectiveSettings.loraSelections)
        assertEquals(listOf(LoraSelection("a.safetensors", 0.5f, 0.25f)), prepared.effectiveParams.loraSelections)
    }

    @Test
    fun switching_the_active_server_after_preparing_still_submits_to_the_captured_one() = runTest {
        val target = storedTargetA()
        val prepared = repository.prepareReferenceGeneration(REFERENCE_ID, target, params)
        dao.deactivateAll()
        dao.activate(rowB.id)

        val job = repository.submitPreparedReference(prepared)

        assertEquals(target, job.target)
        assertEquals(listOf("a:8188"), sent.map { it.hostPort }.distinct())
    }

    @Test
    fun a_deleted_target_fails_without_contacting_the_active_server() = runTest {
        val target = storedTargetA()
        val prepared = repository.prepareReferenceGeneration(REFERENCE_ID, target, params)
        dao.deleteById(rowA.id)
        dao.activate(rowB.id)

        assertFailsWith<DomainException.ConnectionException> { repository.submitPreparedReference(prepared) }
        assertFailsWith<DomainException.ConnectionException> {
            repository.prepareReferenceGeneration(REFERENCE_ID, target, params)
        }
        assertTrue(sent.isEmpty(), sent.toString())
    }

    @Test
    fun an_edited_target_fails_without_contacting_any_server() = runTest {
        val target = storedTargetA()
        val prepared = repository.prepareReferenceGeneration(REFERENCE_ID, target, params)
        val stored = dao.rows.first { it.id == rowA.id }

        for (edited in listOf(stored.copy(hostname = "b"), stored.copy(port = 9999), stored.copy(useHttps = true))) {
            dao.update(edited)
            assertFailsWith<DomainException.ConnectionException>("edited: $edited") {
                repository.submitPreparedReference(prepared)
            }
        }
        assertTrue(sent.isEmpty(), sent.toString())
    }

    @Test
    fun inputs_that_replace_the_built_in_graph_are_rejected() = runTest {
        val target = storedTargetA()
        val rejected = listOf(
            params.copy(customWorkflowJson = """{"1":{"class_type":"X","inputs":{}}}"""),
            params.copy(controlNetEnabled = true, controlNetModel = "canny.pth"),
            params.copy(initImageFilename = "init.png"),
            params.copy(maskImageFilename = "mask.png"),
            params.copy(initImageFilename = "init.png", maskImageFilename = "mask.png"),
        )

        for (input in rejected) {
            assertFailsWith<IllegalArgumentException>("input: $input") {
                repository.prepareReferenceGeneration(REFERENCE_ID, target, input)
            }
        }
        assertEquals(0, random.draws)
        assertTrue(sent.isEmpty(), sent.toString())
    }

    private companion object {
        const val REFERENCE_ID = 7L
        const val RESOLVED_SEED = 123_456_789L
    }
}
