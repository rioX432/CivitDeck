package com.riox432.civitdeck.data.local

import androidx.room.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Upgrades a file-backed database built from the committed version-50 schema through the
 * production migration list. Room validates the migrated tables against the current entities
 * on open, so a migration that diverges from the generated schema fails here.
 */
class SavedReferenceMigrationTest {

    // An explicit temporary file: the default JVM builder points at the real ~/.civitdeck database.
    private val dir: File = Files.createTempDirectory("saved-reference-migration").toFile()
    private val dbFile = File(dir, DB_FILE_NAME)
    private var db: CivitDeckDatabase? = null

    @AfterTest
    fun tearDown() {
        db?.close()
        dir.deleteRecursively()
    }

    @Test
    fun version_50_database_keeps_every_prompt_and_template_field_after_upgrade() = runTest {
        createVersion50Database(LEGACY_ROWS_SQL)

        val rows = openThroughProductionMigrations().savedPromptDao().getAllUserCreated()

        assertEquals(LEGACY_ROWS.sortedBy { it.id }, rows.sortedBy { it.id })
        rows.forEach { assertNull(it.sourceReferenceJson) }
        assertEquals(51, readUserVersion())
        assertEquals(
            ColumnInfo(type = "TEXT", notNull = false, defaultValue = null),
            readColumn("sourceReferenceJson"),
        )
    }

    @Test
    fun upgraded_rows_store_null_or_versioned_json_without_hiding_legacy_rows() = runTest {
        createVersion50Database(LEGACY_ROWS_SQL)
        val dao = openThroughProductionMigrations().savedPromptDao()
        val ordinary = LEGACY_ROWS.first { it.id == ORDINARY_ID }

        dao.upsert(ordinary.copy(sourceReferenceJson = VERSIONED_REFERENCE_JSON))
        dao.insert(ordinary.copy(id = 0, prompt = "new without reference", savedAt = 9_000L))

        val byPrompt = dao.getAllUserCreated().associateBy { it.prompt }
        assertEquals(LEGACY_ROWS.size + 1, byPrompt.size)
        assertEquals(VERSIONED_REFERENCE_JSON, byPrompt.getValue(ordinary.prompt).sourceReferenceJson)
        assertNull(byPrompt.getValue("new without reference").sourceReferenceJson)
        LEGACY_ROWS.filter { it.id != ORDINARY_ID }.forEach { legacy ->
            assertEquals(legacy, byPrompt.getValue(legacy.prompt))
        }
        // Negative IDs are the built-in templates seeded on open, not user rows.
        assertEquals(listOf(TEMPLATE_ID), dao.observeTemplates().first().map { it.id }.filter { it > 0 })
        assertEquals(listOf(AUTO_SAVED_ID), dao.observeHistory().first().map { it.id }.filter { it > 0 })
    }

    private fun openThroughProductionMigrations(): CivitDeckDatabase =
        getRoomDatabase(Room.databaseBuilder<CivitDeckDatabase>(name = dbFile.absolutePath))
            .also { db = it }

    private fun createVersion50Database(rowsSql: List<String>) {
        val schema = Json.parseToJsonElement(File(VERSION_50_SCHEMA).readText())
            .jsonObject.getValue("database").jsonObject
        withConnection { connection ->
            schema.getValue("entities").jsonArray.forEach { element ->
                val entity = element.jsonObject
                val table = entity.getValue("tableName").jsonPrimitive.content
                connection.execSQL(entity.getValue("createSql").jsonPrimitive.content.forTable(table))
                entity["indices"]?.jsonArray?.forEach { index ->
                    connection.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content.forTable(table))
                }
            }
            schema.getValue("setupQueries").jsonArray.forEach { connection.execSQL(it.jsonPrimitive.content) }
            rowsSql.forEach { connection.execSQL(it) }
            connection.execSQL("PRAGMA user_version = 50")
        }
    }

    private fun readUserVersion(): Int {
        closeRoom()
        return withConnection { connection ->
            connection.prepare("PRAGMA user_version").use { statement ->
                statement.step()
                statement.getLong(0).toInt()
            }
        }
    }

    private fun readColumn(name: String): ColumnInfo? {
        closeRoom()
        return withConnection { connection ->
            connection.prepare("PRAGMA table_info(saved_prompts)").use { statement ->
                var found: ColumnInfo? = null
                while (statement.step()) {
                    if (statement.getText(1) == name) {
                        found = ColumnInfo(
                            type = statement.getText(2),
                            notNull = statement.getLong(3) != 0L,
                            defaultValue = if (statement.isNull(4)) null else statement.getText(4),
                        )
                    }
                }
                found
            }
        }
    }

    private fun closeRoom() {
        db?.close()
        db = null
    }

    private fun <T> withConnection(block: (SQLiteConnection) -> T): T {
        val connection = BundledSQLiteDriver().open(dbFile.absolutePath)
        return try {
            block(connection)
        } finally {
            connection.close()
        }
    }

    private fun String.forTable(table: String) = replace("\${TABLE_NAME}", table)

    private data class ColumnInfo(val type: String, val notNull: Boolean, val defaultValue: String?)

    private companion object {
        const val VERSION_50_SCHEMA = "schemas/com.riox432.civitdeck.data.local.CivitDeckDatabase/50.json"
        const val ORDINARY_ID = 1L
        const val TEMPLATE_ID = 2L
        const val AUTO_SAVED_ID = 3L
        const val METADATA_FREE_ID = 4L
        const val VERSIONED_REFERENCE_JSON =
            """{"version":1,"provider":"civitai","artworkId":"123","artworkUrl":"https://civitai.com/images/123"}"""

        val LEGACY_ROWS = listOf(
            SavedPromptEntity(
                id = ORDINARY_ID,
                prompt = "a lighthouse at dusk",
                negativePrompt = "blurry",
                sampler = "Euler a",
                steps = 28,
                cfgScale = 6.5,
                seed = 4_294_967_296L,
                modelName = "SDXL Base",
                size = "832x1216",
                sourceImageUrl = "https://image.civitai.com/x/123.jpeg",
                savedAt = 1_000L,
            ),
            SavedPromptEntity(
                id = TEMPLATE_ID,
                prompt = "portrait of {{subject}}",
                negativePrompt = null,
                sampler = "DPM++ 2M",
                steps = 30,
                cfgScale = 7.0,
                seed = null,
                modelName = null,
                size = null,
                sourceImageUrl = null,
                savedAt = 2_000L,
                isTemplate = true,
                templateName = "Portrait",
                templateVariables = """[{"name":"subject"}]""",
                templateType = "TXT2IMG",
                templateMetadata = """{"category":"people"}""",
                isAppMode = true,
                rawWorkflowJson = """{"3":{"class_type":"KSampler"}}""",
            ),
            SavedPromptEntity(
                id = AUTO_SAVED_ID,
                prompt = "auto saved prompt",
                negativePrompt = null,
                sampler = null,
                steps = null,
                cfgScale = null,
                seed = null,
                modelName = "Flux",
                size = null,
                sourceImageUrl = "https://image.civitai.com/x/456.jpeg",
                savedAt = 3_000L,
                autoSaved = true,
            ),
            // Shaped like a saved artwork whose source supplied no generation metadata.
            SavedPromptEntity(
                id = METADATA_FREE_ID,
                prompt = "",
                negativePrompt = null,
                sampler = null,
                steps = null,
                cfgScale = null,
                seed = null,
                modelName = null,
                size = null,
                sourceImageUrl = "https://image.civitai.com/x/789.jpeg",
                savedAt = 4_000L,
            ),
        )

        // Written as raw v50 SQL so the fixture does not depend on the current entity.
        val LEGACY_ROWS_SQL = listOf(
            "INSERT INTO saved_prompts (id, prompt, negativePrompt, sampler, steps, cfgScale, seed, modelName, " +
                "size, sourceImageUrl, savedAt) VALUES (1, 'a lighthouse at dusk', 'blurry', 'Euler a', 28, 6.5, " +
                "4294967296, 'SDXL Base', '832x1216', 'https://image.civitai.com/x/123.jpeg', 1000)",
            "INSERT INTO saved_prompts (id, prompt, sampler, steps, cfgScale, savedAt, isTemplate, templateName, " +
                "templateVariables, templateType, templateMetadata, isAppMode, rawWorkflowJson) VALUES (2, " +
                "'portrait of {{subject}}', 'DPM++ 2M', 30, 7.0, 2000, 1, 'Portrait', '[{\"name\":\"subject\"}]', " +
                "'TXT2IMG', '{\"category\":\"people\"}', 1, '{\"3\":{\"class_type\":\"KSampler\"}}')",
            "INSERT INTO saved_prompts (id, prompt, modelName, sourceImageUrl, savedAt, autoSaved) VALUES (3, " +
                "'auto saved prompt', 'Flux', 'https://image.civitai.com/x/456.jpeg', 3000, 1)",
            "INSERT INTO saved_prompts (id, prompt, sourceImageUrl, savedAt) VALUES (4, '', " +
                "'https://image.civitai.com/x/789.jpeg', 4000)",
        )
    }
}
