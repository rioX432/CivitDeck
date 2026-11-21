package com.riox432.civitdeck.feature.prompts.data.repository

import androidx.room.Room
import com.riox432.civitdeck.data.local.CivitDeckDatabase
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import com.riox432.civitdeck.data.local.getRoomDatabase
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.SourceGenerationSnapshot
import com.riox432.civitdeck.domain.model.SourceProvider
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.domain.model.SourceReferenceDecodeResult
import com.riox432.civitdeck.domain.model.SourceResource
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.SavedReferenceRepository
import com.riox432.civitdeck.feature.prompts.di.promptsModule
import com.riox432.civitdeck.feature.prompts.presentation.SavedReferenceViewModel
import com.riox432.civitdeck.testing.FakeModelRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SavedReferenceRepositoryTest {

    // An explicit temporary file: the default JVM builder points at the real ~/.civitdeck database.
    private val dir: File = Files.createTempDirectory("saved-reference-repository").toFile()
    private val dbFile = File(dir, "civitdeck.db")
    private var db: CivitDeckDatabase? = null

    @AfterTest
    fun tearDown() {
        db?.close()
        dir.deleteRecursively()
    }

    @Test
    fun complete_and_metadata_free_references_survive_reopening_with_the_same_id() = runTest {
        val first = productionRepository()
        val completeId = first.saveReference(COMPLETE)
        val metadataFreeId = first.saveReference(METADATA_FREE)
        closeDatabase()

        val reopened = productionRepository()
        val complete = requireNotNull(reopened.getReference(completeId))
        val metadataFree = requireNotNull(reopened.observeReference(metadataFreeId).first())

        assertEquals(completeId, complete.id)
        assertEquals(SourceReferenceDecodeResult.Decoded(COMPLETE), complete.sourceReference)
        assertEquals("a lighthouse at dusk", complete.prompt)
        assertEquals("blurry", complete.negativePrompt)
        assertEquals("Euler a", complete.sampler)
        assertEquals(28, complete.steps)
        assertEquals(6.5, complete.cfgScale)
        assertEquals(4_294_967_296L, complete.seed)
        assertEquals("SDXL Base", complete.modelName)
        assertEquals("832x1216", complete.size)
        assertEquals(COMPLETE.mediaUrl, complete.sourceImageUrl)

        assertEquals(metadataFreeId, metadataFree.id)
        assertEquals(SourceReferenceDecodeResult.Decoded(METADATA_FREE), metadataFree.sourceReference)
        assertEquals("", metadataFree.prompt)
        assertNull(metadataFree.sampler)
        assertNull(metadataFree.seed)
        assertNull(metadataFree.sourceImageUrl)
        assertNull(reopened.getReference(MISSING_ID))
    }

    @Test
    fun editing_a_saved_reference_as_a_template_keeps_the_original_snapshot() = runTest {
        val repo = productionRepository()
        val id = repo.saveReference(COMPLETE)
        val stored = requireNotNull(repo.getReference(id))

        // A legacy caller that builds the prompt without the new field must not clear the snapshot.
        repo.saveTemplate(stored.copy(prompt = "edited", isTemplate = true, sourceReference = null))
        repo.saveTemplate(
            stored.copy(
                prompt = "edited again",
                isTemplate = true,
                templateName = "Mine",
                sourceReference = SourceReferenceDecodeResult.Decoded(METADATA_FREE),
            ),
        )

        val row = requireNotNull(repo.getReference(id))
        assertEquals("edited again", row.prompt)
        assertEquals("Mine", row.templateName)
        assertEquals(SourceReferenceDecodeResult.Decoded(COMPLETE), row.sourceReference)
        assertEquals(listOf(id), repo.observeTemplates().first().map { it.id }.filter { it > 0 })
    }

    @Test
    fun legacy_template_updates_keep_unreadable_snapshots_byte_for_byte() = runTest {
        val repo = productionRepository()
        val futureId = insertRaw(prompt = "future", json = FUTURE_JSON)
        val malformedId = insertRaw(prompt = "malformed", json = MALFORMED_JSON)

        listOf(futureId, malformedId).forEach { id ->
            val stored = requireNotNull(repo.getReference(id))
            repo.saveTemplate(stored.copy(prompt = "${stored.prompt} edited", isTemplate = true))
        }

        val dao = requireNotNull(db).savedReferenceDao()
        assertEquals(FUTURE_JSON, dao.getById(futureId)?.sourceReferenceJson)
        assertEquals(MALFORMED_JSON, dao.getById(malformedId)?.sourceReferenceJson)
        assertEquals("future edited", dao.getById(futureId)?.prompt)
    }

    @Test
    fun legacy_corrupt_and_future_rows_stay_listed_and_auto_save_still_deduplicates() = runTest {
        val repo = productionRepository()
        val legacyId = insertRaw(prompt = "legacy", json = null)
        val futureId = insertRaw(prompt = "future", json = FUTURE_JSON)
        val malformedId = insertRaw(prompt = "malformed", json = MALFORMED_JSON)
        val meta = meta(prompt = "auto", model = "Flux")

        repo.autoSave(meta, "https://image.civitai.com/x/1.jpeg")
        repo.autoSave(meta, "https://image.civitai.com/x/1.jpeg")

        val all = repo.observeAll().first().filter { it.id > 0 }.associateBy { it.id }
        assertEquals(4, all.size)
        assertNull(all.getValue(legacyId).sourceReference)
        assertEquals(
            SourceReferenceDecodeResult.UnsupportedVersion(FUTURE_VERSION),
            all.getValue(futureId).sourceReference,
        )
        assertEquals(SourceReferenceDecodeResult.Malformed, all.getValue(malformedId).sourceReference)
        val history = repo.observeHistory().first()
        assertEquals(listOf("auto"), history.map { it.prompt })
        assertNull(history.single().sourceReference)
    }

    @Test
    fun the_one_argument_constructor_keeps_prompt_operations_and_rejects_reference_ones() = runTest {
        val database = openDatabase()
        val repo = SavedPromptRepositoryImpl(database.savedPromptDao())

        repo.save(meta(prompt = "plain"), null)
        val saved = repo.observeAll().first().single { it.prompt == "plain" }
        repo.saveTemplate(saved.copy(isTemplate = true, templateName = "Plain"))

        assertEquals("Plain", repo.observeTemplates().first().single { it.id == saved.id }.templateName)
        assertFailsWith<IllegalStateException> { repo.saveReference(COMPLETE) }
        assertFailsWith<IllegalStateException> { repo.getReference(saved.id) }
        assertFailsWith<IllegalStateException> { repo.observeReference(saved.id) }
    }

    @Test
    fun koin_binds_both_ports_to_one_instance_that_has_the_reference_dao() = runTest {
        val database = openDatabase()
        val koin = koinApplication {
            modules(
                module {
                    single { database }
                    single { database.savedPromptDao() }
                },
                promptsModule,
            )
        }.koin

        val prompts = koin.get<SavedPromptRepository>()
        val references = koin.get<SavedReferenceRepository>()

        assertSame<Any>(prompts, references)
        assertTrue(prompts is SavedPromptRepositoryImpl)
        val id = references.saveReference(METADATA_FREE)
        val stored = requireNotNull(references.getReference(id))
        prompts.saveTemplate(stored.copy(isTemplate = true, sourceReference = null))
        assertEquals(SourceReferenceDecodeResult.Decoded(METADATA_FREE), references.getReference(id)?.sourceReference)
    }

    @Test
    fun koin_resolves_the_saved_reference_view_model_from_the_prompts_module() {
        val database = openDatabase()
        val koin = koinApplication {
            modules(
                module {
                    single { database }
                    single { database.savedPromptDao() }
                    single<ModelRepository> { FakeModelRepository() }
                },
                promptsModule,
            )
        }.koin

        assertTrue(koin.getOrNull<SavedReferenceViewModel>() != null)
    }

    private fun productionRepository(): SavedPromptRepositoryImpl {
        val database = openDatabase()
        return SavedPromptRepositoryImpl(database.savedPromptDao(), database.savedReferenceDao())
    }

    private fun openDatabase(): CivitDeckDatabase =
        getRoomDatabase(Room.databaseBuilder<CivitDeckDatabase>(name = dbFile.absolutePath)).also { db = it }

    private fun closeDatabase() {
        db?.close()
        db = null
    }

    private fun meta(prompt: String, model: String? = null) = ImageGenerationMeta(
        prompt = prompt,
        negativePrompt = null,
        sampler = null,
        cfgScale = null,
        steps = null,
        seed = null,
        model = model,
        size = null,
    )

    private suspend fun insertRaw(prompt: String, json: String?): Long =
        requireNotNull(db).savedReferenceDao().insert(
            SavedPromptEntity(
                prompt = prompt,
                negativePrompt = null,
                sampler = null,
                steps = null,
                cfgScale = null,
                seed = null,
                modelName = null,
                size = null,
                sourceImageUrl = null,
                savedAt = 1_000L,
                sourceReferenceJson = json,
            ),
        )

    private companion object {
        const val MISSING_ID = 9_999L
        const val FUTURE_VERSION = 99

        // Newer-schema and corrupt snapshots must survive edits verbatim; neither is decodable here.
        const val FUTURE_JSON = """{"version":99, "provider":"civitai","unknown":{"nested":[1,2]}}"""
        const val MALFORMED_JSON = """{"version":1,"provider":"civitai","""

        val COMPLETE = SourceReference(
            provider = SourceProvider.Civitai,
            artworkId = "123",
            artworkUrl = SourceReference.civitaiArtworkUrl(123L),
            mediaUrl = "https://image.civitai.com/x/123.jpeg",
            creator = "someone",
            contentRating = "None",
            generation = SourceGenerationSnapshot(
                prompt = "a lighthouse at dusk",
                negativePrompt = "blurry",
                sampler = "Euler a",
                cfgScale = 6.5,
                steps = 28,
                seed = 4_294_967_296L,
                model = "SDXL Base",
                size = "832x1216",
                parameters = mapOf("Clip skip" to "2"),
                opaqueMetadata = mapOf("comfy" to """{"nodes":[]}"""),
            ),
            resources = listOf(SourceResource(versionId = 128_078L, type = "checkpoint", weight = 1.0)),
            usageLinks = listOf("https://civitai.com/images/456"),
        )

        val METADATA_FREE = SourceReference(
            provider = SourceProvider.Civitai,
            artworkId = "789",
            artworkUrl = SourceReference.civitaiArtworkUrl(789L),
        )
    }
}
