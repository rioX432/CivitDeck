package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.data.api.comfyui.ComfyUIOutputImage
import com.riox432.civitdeck.data.api.comfyui.HistoryEntry
import com.riox432.civitdeck.data.api.comfyui.HistoryNodeOutput
import com.riox432.civitdeck.data.api.comfyui.HistoryStatus
import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import com.riox432.civitdeck.domain.repository.ComfyUIHistoryRepository
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIHistoryItemUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIHistoryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComfyUIHistoryUseCaseTest {

    private val testPromptId = "abc-123"
    private val testFilename = "ComfyUI_00001_.png"
    private val testImageUrl = "http://localhost:8188/view?filename=$testFilename&subfolder=&type=output"

    private val fakeImages = listOf(
        ComfyUIGeneratedImage(
            id = "$testPromptId/$testFilename",
            promptId = testPromptId,
            filename = testFilename,
            subfolder = "",
            type = "output",
            imageUrl = testImageUrl,
            meta = com.riox432.civitdeck.domain.model.ComfyUIGenerationMeta(
                positivePrompt = "a cat",
                seed = 12345L,
                samplerName = "euler",
                cfg = 7.0,
                steps = 20,
                loraNames = listOf("detail_tweaker.safetensors"),
            ),
        ),
    )

    private class FakeComfyUIHistoryRepository(
        private val allImages: List<ComfyUIGeneratedImage>,
        private val itemImages: List<ComfyUIGeneratedImage>,
    ) : ComfyUIHistoryRepository {
        override fun fetchHistory(): Flow<List<ComfyUIGeneratedImage>> = flowOf(allImages)
        override fun fetchHistoryItem(promptId: String): Flow<List<ComfyUIGeneratedImage>> =
            flowOf(itemImages)
    }

    @Test
    fun fetchHistory_emits_image_list() = runTest {
        val repo = FakeComfyUIHistoryRepository(allImages = fakeImages, itemImages = emptyList())
        val useCase = FetchComfyUIHistoryUseCase(repo)

        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals(testPromptId, result[0].promptId)
        assertEquals(testFilename, result[0].filename)
        assertEquals(testImageUrl, result[0].imageUrl)
    }

    @Test
    fun fetchHistory_emits_empty_when_no_history() = runTest {
        val repo = FakeComfyUIHistoryRepository(allImages = emptyList(), itemImages = emptyList())
        val useCase = FetchComfyUIHistoryUseCase(repo)

        val result = useCase().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun fetchHistoryItem_emits_images_for_prompt() = runTest {
        val repo = FakeComfyUIHistoryRepository(allImages = emptyList(), itemImages = fakeImages)
        val useCase = FetchComfyUIHistoryItemUseCase(repo)

        val result = useCase(testPromptId).first()

        assertEquals(1, result.size)
        assertEquals(testPromptId, result[0].promptId)
    }

    @Test
    fun fetchHistoryItem_emits_empty_when_not_found() = runTest {
        val repo = FakeComfyUIHistoryRepository(allImages = emptyList(), itemImages = emptyList())
        val useCase = FetchComfyUIHistoryItemUseCase(repo)

        val result = useCase("nonexistent-id").first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun generationMeta_contains_correct_values() = runTest {
        val repo = FakeComfyUIHistoryRepository(allImages = fakeImages, itemImages = emptyList())
        val useCase = FetchComfyUIHistoryUseCase(repo)

        val images = useCase().first()
        val meta = images.first().meta

        assertEquals("a cat", meta.positivePrompt)
        assertEquals(12345L, meta.seed)
        assertEquals("euler", meta.samplerName)
        assertEquals(7.0, meta.cfg)
        assertEquals(20, meta.steps)
        assertEquals(1, meta.loraNames.size)
        assertEquals("detail_tweaker.safetensors", meta.loraNames[0])
    }
}

class ComfyUIHistoryDtoParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun historyEntry_parses_outputs_and_status() {
        val jsonStr = """
            {
              "abc-123": {
                "status": {"status_str": "success", "completed": true},
                "outputs": {
                  "9": {
                    "images": [
                      {"filename": "ComfyUI_00001_.png", "subfolder": "", "type": "output"}
                    ]
                  }
                },
                "prompt": [0, "abc-123", {}, {}, []]
              }
            }
        """.trimIndent()

        val historyMap = json.decodeFromString<Map<String, HistoryEntry>>(jsonStr)

        assertEquals(1, historyMap.size)
        val entry = historyMap["abc-123"]!!
        assertEquals("success", entry.status?.statusStr)
        assertEquals(true, entry.status?.completed)
        assertEquals(1, entry.outputs["9"]?.images?.size)
        assertEquals("ComfyUI_00001_.png", entry.outputs["9"]?.images?.first()?.filename)
    }

    @Test
    fun historyEntry_promptNodes_extracts_node_graph() {
        val nodeGraph = buildJsonObject {
            put(
                "4",
                buildJsonObject {
                    put("class_type", "KSampler")
                    put(
                        "inputs",
                        buildJsonObject {
                            put("seed", 42)
                            put("steps", 20)
                            put("cfg", 7.0)
                            put("sampler_name", "euler")
                        }
                    )
                }
            )
        }
        val promptArray = buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive(0))
            add(kotlinx.serialization.json.JsonPrimitive("abc-123"))
            add(nodeGraph)
        }

        val entry = HistoryEntry(
            status = HistoryStatus(statusStr = "success", completed = true),
            outputs = mapOf(
                "9" to HistoryNodeOutput(
                    images = listOf(ComfyUIOutputImage(filename = "out.png"))
                )
            ),
            prompt = promptArray,
        )

        val nodes = entry.promptNodes
        assertTrue(nodes != null)
        assertTrue(nodes.containsKey("4"))
    }

    @Test
    fun historyEntry_promptNodes_returns_null_when_prompt_absent() {
        val entry = HistoryEntry(
            status = HistoryStatus(statusStr = "success", completed = true),
            outputs = emptyMap(),
            prompt = null,
        )

        assertNull(entry.promptNodes)
    }

    @Test
    fun historyEntry_handles_missing_optional_fields() {
        val jsonStr = """{"abc": {"outputs": {}}}"""
        val historyMap = json.decodeFromString<Map<String, HistoryEntry>>(jsonStr)

        val entry = historyMap["abc"]!!
        assertNull(entry.status)
        assertNull(entry.prompt)
        assertTrue(entry.outputs.isEmpty())
    }
}
