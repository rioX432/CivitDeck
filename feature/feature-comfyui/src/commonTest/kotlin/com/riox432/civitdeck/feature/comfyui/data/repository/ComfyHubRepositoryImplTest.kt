package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyhub.ComfyHubApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.domain.model.ComfyHubCategory
import com.riox432.civitdeck.domain.model.ComfyHubSortOrder
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Covers [ComfyHubRepositoryImpl]: searching/mapping the built-in workflow catalog,
 * fetching detail, and importing a workflow to the connected ComfyUI server.
 */
class ComfyHubRepositoryImplTest {

    private fun repo(comfyUiApi: ComfyUIApi = ComfyUIApi(mockClient { okJson("{}") }, testJson)) =
        ComfyHubRepositoryImpl(ComfyHubApi(), comfyUiApi, testJson)

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
        val api = ComfyUIApi(mockClient { okJson("""{"prompt_id":"imported-1"}""") }, testJson)

        val promptId = repo(api).importToServer("""{"3":{"class_type":"CheckpointLoaderSimple"}}""")

        assertEquals("imported-1", promptId)
    }

    @Test
    fun importToServer_propagates_server_error() = runTest {
        val api = ComfyUIApi(mockClient { respondError(HttpStatusCode.InternalServerError) }, testJson)

        assertFailsWith<Exception> { repo(api).importToServer("""{}""") }
    }
}
