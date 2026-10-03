package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyhub.ComfyHubApi
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyHubCategory
import com.riox432.civitdeck.domain.model.ComfyHubSortOrder
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers [ComfyHubRepositoryImpl]: searching/mapping the built-in workflow catalog,
 * fetching detail, and importing a workflow to the connected ComfyUI server.
 */
class ComfyHubRepositoryImplTest {

    private fun daoWithActive() = FakeComfyUIConnectionDao().apply {
        rows.add(ComfyUIConnectionEntity(id = 1, name = "A", hostname = "h", port = 8188, isActive = true, createdAt = 1))
    }

    private fun repo(
        client: HttpClient = mockClient { okJson("{}") },
        dao: FakeComfyUIConnectionDao = daoWithActive(),
    ) = ComfyHubRepositoryImpl(ComfyHubApi(), ComfyUIApiProvider(dao, client, testJson), testJson)

    @Test
    fun searchWorkflows_maps_dto_to_domain_with_author() = runTest {
        val results = repo().searchWorkflows(
            query = "",
            category = ComfyHubCategory.ALL,
            sort = ComfyHubSortOrder.MOST_DOWNLOADED,
            page = 1,
        )

        assertTrue(results.isNotEmpty())
        val top = results.first()
        assertEquals("ComfyUI", top.author) // creator.username mapped to author
        assertTrue(top.workflowJson.isNotBlank())
    }

    @Test
    fun searchWorkflows_filters_by_category() = runTest {
        val results = repo().searchWorkflows(
            query = "",
            category = ComfyHubCategory.CONTROLNET,
            sort = ComfyHubSortOrder.MOST_DOWNLOADED,
            page = 1,
        )

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.category == "ControlNet" })
    }

    @Test
    fun searchWorkflows_returns_empty_for_unmatched_query() = runTest {
        val results = repo().searchWorkflows(
            query = "no-such-workflow-xyz",
            category = ComfyHubCategory.ALL,
            sort = ComfyHubSortOrder.MOST_DOWNLOADED,
            page = 1,
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun getWorkflowDetail_returns_matching_workflow() = runTest {
        val detail = repo().getWorkflowDetail("std-txt2img")

        assertEquals("std-txt2img", detail.id)
        assertEquals("Standard txt2img", detail.name)
    }

    @Test
    fun importToServer_posts_prompt_and_returns_prompt_id() = runTest {
        val client = mockClient { okJson("""{"prompt_id":"imported-1"}""") }

        val promptId = repo(client).importToServer("""{"3":{"class_type":"CheckpointLoaderSimple"}}""")

        assertEquals("imported-1", promptId)
    }

    @Test
    fun importToServer_posts_to_the_active_connection_url() = runTest {
        var request: HttpRequestData? = null
        val client = mockClient {
            request = it
            okJson("""{"prompt_id":"imported-1"}""")
        }
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

        repo(client, dao).importToServer("""{}""")

        val sent = assertNotNull(request)
        assertEquals(HttpMethod.Post, sent.method)
        assertEquals("https://home.local:8443/prompt", sent.url.toString())
    }

    @Test
    fun importToServer_throws_ConnectionException_without_active_connection() = runTest {
        var requested = false
        val client = mockClient {
            requested = true
            okJson("""{"prompt_id":"imported-1"}""")
        }

        assertFailsWith<DomainException.ConnectionException> {
            repo(client, FakeComfyUIConnectionDao()).importToServer("""{}""")
        }
        assertFalse(requested)
    }

    @Test
    fun importToServer_propagates_server_error() = runTest {
        val client = mockClient { respondError(HttpStatusCode.InternalServerError) }

        assertFailsWith<Exception> { repo(client).importToServer("""{}""") }
    }
}
