package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.local.dao.ComfyUIConnectionDao
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyUIBoundHistoryItem
import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import com.riox432.civitdeck.domain.model.ComfyUIGenerationMeta
import com.riox432.civitdeck.domain.model.ComfyUIHistoryOutcome
import com.riox432.civitdeck.domain.model.ComfyUIHistoryPage
import com.riox432.civitdeck.domain.model.ComfyUIJobHandle
import com.riox432.civitdeck.domain.model.ComfyUIJobTarget
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.domain.repository.ComfyUIHistoryRepository
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers which server [ComfyUIHistoryRepositoryImpl] reads history from and attaches to outputs,
 * and how a bound lookup reports the remote outcome.
 */
class ComfyUIHistoryTargetTest {

    private val targetA = ComfyUIJobTarget(1, "a.local", 8188, false, false, null)
    private val targetB = ComfyUIJobTarget(2, "b.local", 8188, false, false, null)

    private fun twoServers(active: Long) = FakeComfyUIConnectionDao().apply {
        rows.add(ComfyUIConnectionEntity(id = 1, name = "A", hostname = "a.local", port = 8188, isActive = active == 1L, createdAt = 1))
        rows.add(ComfyUIConnectionEntity(id = 2, name = "B", hostname = "b.local", port = 8188, isActive = active == 2L, createdAt = 2))
    }

    // Same order as ComfyUIConnectionRepositoryImpl.activateConnection; the fake's activate alone keeps others active.
    private suspend fun FakeComfyUIConnectionDao.switchTo(id: Long) {
        deactivateAll()
        activate(id)
    }

    private class Recorder {
        val hosts = mutableListOf<String>()
    }

    private fun repo(
        dao: FakeComfyUIConnectionDao,
        recorder: Recorder = Recorder(),
        onRequest: suspend () -> Unit = {},
        body: (host: String) -> String,
    ) = ComfyUIHistoryRepositoryImpl(
        ComfyUIApiProvider(
            dao,
            mockClient { request ->
                recorder.hosts += request.url.host
                onRequest()
                okJson(body(request.url.host))
            },
            testJson,
        ),
    )

    @Test
    fun same_prompt_and_filename_on_two_servers_keep_their_own_targets() = runTest {
        val dao = twoServers(active = 1)
        val repo = repo(dao) { historyEntry(PROMPT, status = SUCCESS, image = "out.png") }

        val fromA = repo.fetchHistory(PAGE_SIZE).first().images.single()
        dao.switchTo(2)
        val fromB = repo.fetchHistory(PAGE_SIZE).first().images.single()

        assertEquals(fromA.id, fromB.id)
        assertEquals(targetA, fromA.jobTarget)
        assertEquals(targetB, fromB.jobTarget)
        assertTrue(fromA.imageUrl.startsWith("http://a.local:8188/view?"))
        assertTrue(fromB.imageUrl.startsWith("http://b.local:8188/view?"))
    }

    @Test
    fun activating_another_server_during_a_page_fetch_keeps_the_target_captured_at_the_start() = runTest {
        val dao = twoServers(active = 1)
        val recorder = Recorder()
        val repo = repo(dao, recorder, onRequest = { dao.switchTo(2) }) {
            historyEntry(PROMPT, status = SUCCESS, image = "out.png")
        }

        val image = repo.fetchHistory(PAGE_SIZE).first().images.single()

        assertEquals(listOf("a.local"), recorder.hosts)
        assertEquals(targetA, image.jobTarget)
    }

    @Test
    fun activating_another_server_during_a_prompt_lookup_keeps_the_target_captured_at_the_start() = runTest {
        val dao = twoServers(active = 1)
        val recorder = Recorder()
        val repo = repo(dao, recorder, onRequest = { dao.switchTo(2) }) {
            historyEntry(PROMPT, status = SUCCESS, image = "out.png")
        }

        val image = repo.fetchHistoryItem(PROMPT).first().single()

        assertEquals(listOf("a.local"), recorder.hosts)
        assertEquals(targetA, image.jobTarget)
    }

    @Test
    fun bound_lookup_reads_the_jobs_server_while_another_is_active() = runTest {
        val recorder = Recorder()
        val repo = repo(twoServers(active = 2), recorder) { historyEntry(PROMPT, status = SUCCESS, image = "out.png") }
        val job = ComfyUIJobHandle(targetA, PROMPT)

        val item = repo.fetchHistoryItem(job).first()

        assertEquals(listOf("a.local"), recorder.hosts)
        assertEquals(job, item.job)
        assertTrue(item.present)
        assertEquals(ComfyUIHistoryOutcome.Completed, item.outcome)
        assertEquals(targetA, item.images.single().jobTarget)
        assertTrue(item.images.single().imageUrl.startsWith("http://a.local:8188/view?"))
    }

    @Test
    fun output_built_without_a_target_stays_unlinked() {
        val image = ComfyUIGeneratedImage(
            id = "p1/out.png",
            promptId = "p1",
            filename = "out.png",
            subfolder = "",
            type = "output",
            imageUrl = "http://a.local:8188/view?filename=out.png",
            meta = ComfyUIGenerationMeta(),
        )

        assertNull(image.jobTarget)
    }

    @Test
    fun success_without_images_is_completed() = runTest {
        val item = boundLookup(historyEntry(PROMPT, status = SUCCESS, messages = """[["execution_success", {"prompt_id": "$PROMPT"}]]"""))

        assertTrue(item.present)
        assertEquals(ComfyUIHistoryOutcome.Completed, item.outcome)
        assertTrue(item.images.isEmpty())
        assertNull(item.error)
    }

    @Test
    fun execution_error_is_failed_with_the_servers_message() = runTest {
        val item = boundLookup(historyEntry(PROMPT, status = ERROR, messages = executionError("CUDA out of memory")))

        assertEquals(ComfyUIHistoryOutcome.Failed, item.outcome)
        assertEquals("CUDA out of memory", item.error)
        assertTrue(item.images.isEmpty())
    }

    @Test
    fun failed_entry_with_partial_images_is_failed_not_completed() = runTest {
        val item = boundLookup(
            historyEntry(PROMPT, status = ERROR, messages = executionError("node 12 failed"), image = "partial.png"),
        )

        assertEquals(ComfyUIHistoryOutcome.Failed, item.outcome)
        assertEquals(listOf("partial.png"), item.images.map { it.filename })
    }

    @Test
    fun execution_interrupted_is_interrupted_not_failed() = runTest {
        val item = boundLookup(
            historyEntry(PROMPT, status = ERROR, messages = """[["execution_interrupted", {"prompt_id": "$PROMPT", "node_id": "3"}]]"""),
        )

        assertEquals(ComfyUIHistoryOutcome.Interrupted, item.outcome)
        assertNull(item.error)
        assertTrue(item.images.isEmpty())
    }

    @Test
    fun interruption_recorded_for_another_prompt_does_not_make_this_one_interrupted() = runTest {
        val item = boundLookup(
            historyEntry(PROMPT, status = ERROR, messages = """[["execution_interrupted", {"prompt_id": "other"}]]"""),
        )

        assertEquals(ComfyUIHistoryOutcome.Failed, item.outcome)
    }

    @Test
    fun error_status_without_any_message_is_failed() = runTest {
        val item = boundLookup(historyEntry(PROMPT, status = ERROR))

        assertEquals(ComfyUIHistoryOutcome.Failed, item.outcome)
        assertNull(item.error)
    }

    @Test
    fun unknown_or_missing_status_is_unknown_even_with_images() = runTest {
        val statuses = listOf(
            """"status": {"status_str": "running", "completed": false}""",
            """"status": {}""",
            null,
        )
        for (status in statuses) {
            val item = boundLookup(historyEntry(PROMPT, status = status, image = "out.png"))

            assertTrue(item.present)
            assertEquals(ComfyUIHistoryOutcome.Unknown, item.outcome, "status: $status")
        }
    }

    @Test
    fun malformed_messages_are_ignored_and_do_not_decide_the_outcome() = runTest {
        val malformed = listOf(
            """{"execution_interrupted": {"prompt_id": "$PROMPT"}}""",
            """[["execution_interrupted"], "execution_error", [3, {"prompt_id": "$PROMPT"}]]""",
        )
        for (messages in malformed) {
            val unknown = boundLookup(historyEntry(PROMPT, status = """"status": {"status_str": "running"}""", messages = messages))
            val failed = boundLookup(historyEntry(PROMPT, status = ERROR, messages = messages))

            assertEquals(ComfyUIHistoryOutcome.Unknown, unknown.outcome, "messages: $messages")
            assertEquals(ComfyUIHistoryOutcome.Failed, failed.outcome, "messages: $messages")
        }
    }

    @Test
    fun missing_history_entry_is_absent_and_unknown() = runTest {
        val item = boundLookup("{}")

        assertFalse(item.present)
        assertEquals(ComfyUIHistoryOutcome.Unknown, item.outcome)
        assertTrue(item.images.isEmpty())
    }

    @Test
    fun network_error_in_a_bound_lookup_propagates_instead_of_reporting_absence() = runTest {
        val provider = ComfyUIApiProvider(
            twoServers(active = 1),
            mockClient { respondError(HttpStatusCode.InternalServerError) },
            testJson,
        )

        assertFailsWith<Exception> {
            ComfyUIHistoryRepositoryImpl(provider).fetchHistoryItem(ComfyUIJobHandle(targetA, PROMPT)).first()
        }
    }

    @Test
    fun edited_or_deleted_target_fails_without_contacting_any_server() = runTest {
        val edited = twoServers(active = 1).apply { rows[0] = rows[0].copy(hostname = "a-new.local") }
        val deleted = twoServers(active = 1).apply { rows.removeAt(0) }

        for (dao in listOf(edited, deleted)) {
            val recorder = Recorder()
            val repo = repo(dao, recorder) { historyEntry(PROMPT, status = SUCCESS) }

            assertFailsWith<DomainException.ConnectionException> {
                repo.fetchHistoryItem(ComfyUIJobHandle(targetA, PROMPT)).first()
            }
            assertTrue(recorder.hosts.isEmpty())
        }
    }

    @Test
    fun connection_edited_between_capture_and_request_fails_instead_of_using_the_new_address() = runTest {
        val fake = twoServers(active = 1)
        // Edits the active row right after the repository has read it.
        val dao = object : ComfyUIConnectionDao by fake {
            override suspend fun getActive() = fake.getActive().also { fake.rows[0] = fake.rows[0].copy(port = 9999) }
        }
        val recorder = Recorder()
        val repo = ComfyUIHistoryRepositoryImpl(
            ComfyUIApiProvider(
                dao,
                mockClient { request ->
                    recorder.hosts += request.url.host
                    okJson(historyEntry(PROMPT, status = SUCCESS))
                },
                testJson,
            ),
        )

        assertFailsWith<DomainException.ConnectionException> { repo.fetchHistory(PAGE_SIZE).first() }
        assertTrue(recorder.hosts.isEmpty())
    }

    @Test
    fun repository_without_bound_support_fails_on_collection_without_reading_the_active_server() = runTest {
        val legacy = LegacyOnlyRepository()
        val flow = legacy.fetchHistoryItem(ComfyUIJobHandle(targetA, PROMPT))

        assertFailsWith<UnsupportedOperationException> { flow.first() }
        assertTrue(legacy.calls.isEmpty())
    }

    private class LegacyOnlyRepository : ComfyUIHistoryRepository {
        val calls = mutableListOf<String>()

        override fun fetchHistory(maxItems: Int): Flow<ComfyUIHistoryPage> {
            calls += "fetchHistory"
            return flowOf(ComfyUIHistoryPage(emptyList(), hasMore = false))
        }

        override fun fetchHistoryItem(promptId: String): Flow<List<ComfyUIGeneratedImage>> {
            calls += "fetchHistoryItem($promptId)"
            return flowOf(emptyList())
        }
    }

    private suspend fun boundLookup(body: String): ComfyUIBoundHistoryItem {
        val item = repo(twoServers(active = 2)) { body }.fetchHistoryItem(ComfyUIJobHandle(targetA, PROMPT)).first()
        assertNotEquals(targetB, item.job.target)
        return item
    }

    private fun executionError(message: String) =
        """[["execution_start", {"prompt_id": "$PROMPT"}], ["execution_error", {"prompt_id": "$PROMPT", "exception_message": "$message"}]]"""

    /** One history entry; [status] is the raw `"status": {...}` member, or null to leave it out. */
    private fun historyEntry(
        promptId: String,
        status: String?,
        messages: String? = null,
        image: String? = null,
    ): String {
        val statusMember = when {
            status == null -> null
            messages == null -> status
            else -> {
                val fields = status.substringAfter("{").substringBeforeLast("}").trim()
                val separator = if (fields.isEmpty()) "" else ", "
                """"status": {$fields$separator"messages": $messages}"""
            }
        }
        val outputs = image?.let { """"9": {"images": [{"filename": "$it", "subfolder": "", "type": "output"}]}""" }.orEmpty()
        val members = listOfNotNull(statusMember, """"outputs": {$outputs}""").joinToString(", ")
        return """{"$promptId": {$members}}"""
    }

    private companion object {
        const val PROMPT = "p1"
        const val PAGE_SIZE = 200
        const val SUCCESS = """"status": {"status_str": "success", "completed": true}"""
        const val ERROR = """"status": {"status_str": "error", "completed": false}"""
    }
}
