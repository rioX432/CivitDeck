package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

    private fun repo(dao: FakeComfyUIConnectionDao, client: HttpClient) =
        ComfyUIHistoryRepositoryImpl(ComfyUIApiProvider(dao, client, testJson))

    private fun repo(dao: FakeComfyUIConnectionDao, body: String) = repo(dao, mockClient { okJson(body) })

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
        val repo = repo(daoWithActive(), historyBody)

        val images = repo.fetchHistory(PAGE_SIZE).first().images

        assertEquals(1, images.size)
        val image = images.first()
        assertEquals("p1/out.png", image.id)
        assertEquals("p1", image.promptId)
        assertEquals("out.png", image.filename)
        assertTrue(image.imageUrl.contains("filename=out.png"))
        assertTrue(image.imageUrl.endsWith("&_prompt=p1"))
    }

    @Test
    fun fetchHistory_requests_and_builds_view_urls_on_the_active_connection() = runTest {
        val dao = FakeComfyUIConnectionDao().apply {
            rows.add(ComfyUIConnectionEntity(id = 1, name = "Old", hostname = "old", port = 8188, createdAt = 1))
            rows.add(
                ComfyUIConnectionEntity(
                    id = 2,
                    name = "Home",
                    hostname = "home.local",
                    port = 8443,
                    useHttps = true,
                    isActive = true,
                    createdAt = 2,
                ),
            )
        }
        var request: HttpRequestData? = null
        val client = mockClient {
            request = it
            okJson(historyBody)
        }

        val image = repo(dao, client).fetchHistory(PAGE_SIZE).first().images.single()

        val url = assertNotNull(request).url
        assertEquals("home.local:8443", "${url.host}:${url.port}")
        assertTrue(image.imageUrl.startsWith("https://home.local:8443/view?"))
    }

    @Test
    fun fetchHistory_extracts_generation_meta_from_prompt_nodes() = runTest {
        val repo = repo(daoWithActive(), historyBody)

        val meta = repo.fetchHistory(PAGE_SIZE).first().images.first().meta

        assertEquals("a cat", meta.positivePrompt)
        assertEquals(123L, meta.seed)
        assertEquals(7.5, meta.cfg)
        assertEquals(25, meta.steps)
        assertEquals("euler", meta.samplerName)
        assertEquals(listOf("myLora.safetensors"), meta.loraNames)
    }

    @Test
    fun fetchHistory_requests_only_the_newest_maxItems_entries() = runTest {
        var request: HttpRequestData? = null
        val client = mockClient {
            request = it
            okJson(historyBody)
        }
        val repo = repo(daoWithActive(), client)

        repo.fetchHistory(PAGE_SIZE).first()

        val url = assertNotNull(request).url
        assertTrue(url.encodedPath.endsWith("/history"))
        assertEquals(PAGE_SIZE.toString(), url.parameters["max_items"])
    }

    @Test
    fun fetchHistory_hasMore_when_server_returns_exactly_maxItems_entries() = runTest {
        val repo = repo(daoWithActive(), historyOf("p1", "p2"))

        val page = repo.fetchHistory(maxItems = 2).first()

        assertTrue(page.hasMore)
        assertEquals(2, page.images.size)
    }

    @Test
    fun fetchHistory_no_more_when_server_returns_fewer_than_maxItems_entries() = runTest {
        val repo = repo(daoWithActive(), historyOf("p1"))

        val page = repo.fetchHistory(maxItems = 2).first()

        assertFalse(page.hasMore)
        assertEquals(1, page.images.size)
    }

    @Test
    fun fetchHistory_counts_entry_without_outputs_toward_maxItems_but_adds_no_image() = runTest {
        val body = """
            {
              "failed": {"status": {"status_str": "error", "completed": false}, "outputs": {}},
              "p1": {"outputs": {"9": {"images": [{"filename": "p1.png", "subfolder": "", "type": "output"}]}}}
            }
        """.trimIndent()
        val repo = repo(daoWithActive(), body)

        val page = repo.fetchHistory(maxItems = 2).first()

        assertTrue(page.hasMore)
        assertEquals(listOf("p1/p1.png"), page.images.map { it.id })
    }

    @Test
    fun fetchHistory_returns_empty_when_history_is_empty() = runTest {
        val repo = repo(daoWithActive(), "{}")

        val images = repo.fetchHistory(PAGE_SIZE).first().images

        assertTrue(images.isEmpty())
    }

    @Test
    fun fetchHistoryItem_returns_images_for_single_prompt() = runTest {
        val repo = repo(daoWithActive(), historyBody)

        val images = repo.fetchHistoryItem("p1").first()

        assertEquals(1, images.size)
        assertEquals("p1/out.png", images.first().id)
    }

    @Test
    fun fetchHistoryItem_returns_empty_when_prompt_absent() = runTest {
        val repo = repo(daoWithActive(), "{}")

        val images = repo.fetchHistoryItem("missing").first()

        assertTrue(images.isEmpty())
    }

    @Test
    fun fetchHistory_throws_ConnectionException_without_active_connection() = runTest {
        val repo = repo(FakeComfyUIConnectionDao(), historyBody)

        assertFailsWith<DomainException.ConnectionException> {
            repo.fetchHistory(PAGE_SIZE).first()
        }
    }

    @Test
    fun fetchHistory_propagates_api_error() = runTest {
        val repo = repo(daoWithActive(), mockClient { respondError(HttpStatusCode.InternalServerError) })

        assertFailsWith<Exception> { repo.fetchHistory(PAGE_SIZE).first() }
    }

    private fun historyOf(vararg promptIds: String): String = promptIds.joinToString(
        prefix = "{",
        postfix = "}",
    ) { id ->
        """"$id": {"outputs": {"9": {"images": [{"filename": "$id.png", "subfolder": "", "type": "output"}]}}}"""
    }

    private companion object {
        const val PAGE_SIZE = 200
    }
}
