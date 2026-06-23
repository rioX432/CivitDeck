package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import com.riox432.civitdeck.domain.model.DomainException

/**
 * Covers [ComfyUIHistoryRepositoryImpl]: flattening history outputs into
 * [com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage]s, generation-meta
 * extraction (KSampler/CLIPTextEncode/LoraLoader scan), and the active-connection guard.
 */
class ComfyUIHistoryRepositoryImplTest {

    private fun daoWithActive() = FakeComfyUIConnectionDao().apply {
        rows.add(ComfyUIConnectionEntity(id = 1, name = "A", hostname = "h", port = 8188, isActive = true, createdAt = 1))
    }

    private fun api(body: String) = ComfyUIApi(mockClient { okJson(body) }, testJson)

    // History entry with one output image and a prompt graph carrying KSampler/CLIP/Lora nodes.
    private val historyBody = """
        {
          "p1": {
            "status": {"status_str": "success", "completed": true},
            "outputs": {"9": {"images": [{"filename": "out.png", "subfolder": "", "type": "output"}]}},
            "prompt": [
              0, "p1",
              {
                "4": {"class_type": "KSampler", "inputs": {"seed": 123, "cfg": 7.5, "steps": 25, "sampler_name": "euler"}},
                "6": {"class_type": "CLIPTextEncode", "inputs": {"text": "a cat"}},
                "10": {"class_type": "LoraLoader", "inputs": {"lora_name": "myLora.safetensors"}}
              }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun fetchHistory_maps_output_image_with_url_and_id() = runTest {
        val repo = ComfyUIHistoryRepositoryImpl(daoWithActive(), api(historyBody))

        val images = repo.fetchHistory().first()

        assertEquals(1, images.size)
        val image = images.first()
        assertEquals("p1/out.png", image.id)
        assertEquals("p1", image.promptId)
        assertEquals("out.png", image.filename)
        assertTrue(image.imageUrl.contains("filename=out.png"))
        assertTrue(image.imageUrl.endsWith("&_prompt=p1"))
    }

    @Test
    fun fetchHistory_extracts_generation_meta_from_prompt_nodes() = runTest {
        val repo = ComfyUIHistoryRepositoryImpl(daoWithActive(), api(historyBody))

        val meta = repo.fetchHistory().first().first().meta

        assertEquals("a cat", meta.positivePrompt)
        assertEquals(123L, meta.seed)
        assertEquals(7.5, meta.cfg)
        assertEquals(25, meta.steps)
        assertEquals("euler", meta.samplerName)
        assertEquals(listOf("myLora.safetensors"), meta.loraNames)
    }

    @Test
    fun fetchHistory_returns_empty_when_history_is_empty() = runTest {
        val repo = ComfyUIHistoryRepositoryImpl(daoWithActive(), api("{}"))

        val images = repo.fetchHistory().first()

        assertTrue(images.isEmpty())
    }

    @Test
    fun fetchHistoryItem_returns_images_for_single_prompt() = runTest {
        val repo = ComfyUIHistoryRepositoryImpl(daoWithActive(), api(historyBody))

        val images = repo.fetchHistoryItem("p1").first()

        assertEquals(1, images.size)
        assertEquals("p1/out.png", images.first().id)
    }

    @Test
    fun fetchHistoryItem_returns_empty_when_prompt_absent() = runTest {
        val repo = ComfyUIHistoryRepositoryImpl(daoWithActive(), api("{}"))

        val images = repo.fetchHistoryItem("missing").first()

        assertTrue(images.isEmpty())
    }

    @Test
    fun fetchHistory_throws_ConnectionException_without_active_connection() = runTest {
        val repo = ComfyUIHistoryRepositoryImpl(FakeComfyUIConnectionDao(), api(historyBody))

        assertFailsWith<DomainException.ConnectionException> {
            repo.fetchHistory().first()
        }
    }

    @Test
    fun fetchHistory_propagates_api_error() = runTest {
        val errorApi = ComfyUIApi(
            mockClient { respondError(HttpStatusCode.InternalServerError) },
            testJson,
        )
        val repo = ComfyUIHistoryRepositoryImpl(daoWithActive(), errorApi)

        assertFailsWith<Exception> { repo.fetchHistory().first() }
    }
}
