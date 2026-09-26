package com.riox432.civitdeck.data.api.comfyui

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers when the ComfyUI progress stream ends. It must end only on messages ComfyUI sends after
 * writing the prompt's `/history` entry: `executing` with a null node for our prompt, or a `status`
 * with an empty queue. `execution_success` arrives before that write, so ending on it lets the
 * final history read miss the entry and report a failed generation.
 */
class ComfyUIWebSocketTerminalTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val api = ComfyUIWebSocketApi(HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) }), json)

    private fun ends(msg: ComfyUIWebSocketMessage) = isTerminalMessage(msg, PROMPT_ID)

    @Test
    fun executing_with_null_node_for_our_prompt_ends_the_stream() {
        assertTrue(ends(ComfyUIWebSocketMessage.Executing(PROMPT_ID, null)))
    }

    @Test
    fun status_with_empty_queue_ends_the_stream() {
        assertTrue(ends(ComfyUIWebSocketMessage.Status(0)))
    }

    @Test
    fun executing_a_node_does_not_end_the_stream() {
        assertFalse(ends(ComfyUIWebSocketMessage.Executing(PROMPT_ID, "3")))
    }

    @Test
    fun executing_with_null_node_for_another_prompt_does_not_end_the_stream() {
        assertFalse(ends(ComfyUIWebSocketMessage.Executing("other-prompt", null)))
    }

    @Test
    fun status_with_prompts_remaining_does_not_end_the_stream() {
        assertFalse(ends(ComfyUIWebSocketMessage.Status(2)))
    }

    @Test
    fun execution_success_and_error_do_not_end_the_stream() {
        assertFalse(ends(ComfyUIWebSocketMessage.ExecutionSuccess(PROMPT_ID)))
        assertFalse(ends(ComfyUIWebSocketMessage.ExecutionError(PROMPT_ID, "boom")))
    }

    @Test
    fun status_frame_without_queue_remaining_parses_to_a_null_count_and_does_not_end_the_stream() {
        val frames = listOf(
            """{"type":"status","data":{"status":{"exec_info":{}},"sid":"abc"}}""",
            """{"type":"status","data":{"sid":"abc"}}""",
        )
        frames.forEach { frame ->
            val msg = assertNotNull(api.parseMessage(frame), frame)
            assertEquals(ComfyUIWebSocketMessage.Status(null), msg, frame)
            assertFalse(ends(msg), frame)
        }
    }

    @Test
    fun parsed_end_frames_end_the_stream() {
        val status = assertNotNull(
            api.parseMessage("""{"type":"status","data":{"status":{"exec_info":{"queue_remaining":0}}}}"""),
        )
        val executing = assertNotNull(
            api.parseMessage("""{"type":"executing","data":{"node":null,"prompt_id":"$PROMPT_ID"}}"""),
        )

        assertEquals(ComfyUIWebSocketMessage.Status(0), status)
        assertEquals(ComfyUIWebSocketMessage.Executing(PROMPT_ID, null), executing)
        assertTrue(ends(status))
        assertTrue(ends(executing))
    }

    private companion object {
        const val PROMPT_ID = "prompt-1"
    }
}
