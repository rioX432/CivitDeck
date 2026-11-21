package com.riox432.civitdeck.data.local

import androidx.room.Room
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavedReferenceDaoTest {

    private val db = getRoomDatabase(Room.inMemoryDatabaseBuilder<CivitDeckDatabase>())
    private val dao = db.savedReferenceDao()

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun inserted_references_get_distinct_ids_and_each_id_observes_only_its_row() = runTest {
        val first = dao.insert(reference(prompt = "first"))
        val second = dao.insert(reference(prompt = "second"))

        assertTrue(first > 0 && second > 0)
        assertNotEquals(first, second)
        assertEquals("first", dao.getById(first)?.prompt)
        assertEquals("second", dao.getById(second)?.prompt)
        assertEquals(first, dao.observeById(first).first()?.id)
        assertEquals("second", dao.observeById(second).first()?.prompt)
    }

    @Test
    fun a_missing_id_reads_and_observes_null() = runTest {
        dao.insert(reference(prompt = "present"))

        assertNull(dao.getById(MISSING_ID))
        assertNull(dao.observeById(MISSING_ID).first())
    }

    @Test
    fun capture_stores_a_snapshot_once_and_leaves_editable_fields_alone() = runTest {
        val id = dao.insert(reference(prompt = "editable", sampler = "Euler a"))

        assertEquals(1, dao.captureSourceIfAbsent(id, SNAPSHOT_V1))
        assertEquals(0, dao.captureSourceIfAbsent(id, SNAPSHOT_OTHER))
        assertEquals(0, dao.captureSourceIfAbsent(MISSING_ID, SNAPSHOT_V1))

        val expected = reference(prompt = "editable", sampler = "Euler a")
            .copy(id = id, sourceReferenceJson = SNAPSHOT_V1)
        assertEquals(expected, dao.getById(id))
    }

    @Test
    fun template_upsert_keeps_the_original_snapshot_even_when_the_caller_omits_or_changes_it() = runTest {
        val id = dao.insert(reference(prompt = "before", sourceReferenceJson = SNAPSHOT_V1))
        val stored = requireNotNull(dao.getById(id))

        dao.upsertPreservingSource(stored.copy(isTemplate = true, templateName = "Mine", sourceReferenceJson = null))
        dao.upsertPreservingSource(
            stored.copy(prompt = "after", isTemplate = true, sourceReferenceJson = SNAPSHOT_OTHER),
        )

        val row = requireNotNull(dao.getById(id))
        assertEquals("after", row.prompt)
        assertEquals(true, row.isTemplate)
        assertEquals(SNAPSHOT_V1, row.sourceReferenceJson)
    }

    @Test
    fun template_upsert_keeps_opaque_future_version_json_verbatim() = runTest {
        val id = dao.insert(reference(prompt = "future", sourceReferenceJson = SNAPSHOT_FUTURE))

        dao.upsertPreservingSource(requireNotNull(dao.getById(id)).copy(prompt = "edited", sourceReferenceJson = null))

        assertEquals(SNAPSHOT_FUTURE, dao.getById(id)?.sourceReferenceJson)
    }

    @Test
    fun template_upsert_without_a_stored_snapshot_saves_what_the_caller_supplies() = runTest {
        val legacyId = dao.insert(reference(prompt = "legacy"))

        dao.upsertPreservingSource(requireNotNull(dao.getById(legacyId)).copy(prompt = "legacy edited"))
        dao.upsertPreservingSource(reference(prompt = "new template", sourceReferenceJson = SNAPSHOT_V1))

        assertEquals("legacy edited", dao.getById(legacyId)?.prompt)
        assertNull(dao.getById(legacyId)?.sourceReferenceJson)
        val created = db.savedPromptDao().getAllUserCreated().single { it.prompt == "new template" }
        assertNotEquals(legacyId, created.id)
        assertEquals(SNAPSHOT_V1, created.sourceReferenceJson)
    }

    @Test
    fun deleting_through_the_existing_prompt_dao_removes_the_reference() = runTest {
        val id = dao.insert(reference(prompt = "to delete", sourceReferenceJson = SNAPSHOT_V1))

        assertEquals(1, db.savedPromptDao().deleteById(id))

        assertNull(dao.observeById(id).first())
    }

    private fun reference(
        prompt: String,
        sampler: String? = null,
        sourceReferenceJson: String? = null,
    ) = SavedPromptEntity(
        prompt = prompt,
        negativePrompt = null,
        sampler = sampler,
        steps = null,
        cfgScale = null,
        seed = null,
        modelName = null,
        size = null,
        sourceImageUrl = "https://image.civitai.com/x/123.jpeg",
        savedAt = 1_000L,
        sourceReferenceJson = sourceReferenceJson,
    )

    private companion object {
        const val MISSING_ID = 9_999L
        const val SNAPSHOT_V1 = """{"version":1,"provider":"civitai","artworkId":"123"}"""
        const val SNAPSHOT_OTHER = """{"version":1,"provider":"civitai","artworkId":"456"}"""

        // A newer schema this build cannot decode must still survive edits byte for byte.
        const val SNAPSHOT_FUTURE = """{"version":99, "provider":"civitai","unknown":{"nested":[1,2]}}"""
    }
}
