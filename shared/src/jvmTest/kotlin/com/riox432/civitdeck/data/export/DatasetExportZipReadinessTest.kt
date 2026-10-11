package com.riox432.civitdeck.data.export

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.domain.model.ExportFormat
import com.riox432.civitdeck.domain.model.ExportProgress
import io.ktor.client.request.get
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class DatasetExportZipReadinessTest : DatasetExportTestFixture() {
    private suspend fun realZipReadability(): Pair<Boolean, Boolean> {
        val dir = java.nio.file.Files.createTempDirectory("civitdeck-synthetic-export-").toFile()
        var readableAtCompleted = false
        var readableAfterCollection = false
        try {
            client(ApiKeyProvider(), mutableListOf()).use { httpClient ->
                val repository = ExportRepositoryImpl(
                    FakeDatasetRepo(
                        listOf(DatasetCollection(1, "clean_room", createdAt = 0, updatedAt = 0)),
                        listOf(image())
                    ),
                    httpClient,
                    DatasetZipWriterFactoryImpl(),
                    object : ExportPathProvider { override fun getExportCacheDirectory() = dir.absolutePath },
                )
                repository.exportDataset(1, ExportFormat.ZIP).collect { event ->
                    if (event is ExportProgress.Completed) readableAtCompleted = verifyZip(event.outputPath)
                }
                readableAfterCollection = verifyZip(java.io.File(dir, "clean_room.zip").absolutePath)
            }
        } finally { dir.deleteRecursively() }
        return readableAtCompleted to readableAfterCollection
    }

    private fun verifyZip(path: String): Boolean = try {
        java.util.zip.ZipFile(path).use { zip ->
            val entries = zip.entries().toList()
            check(entries.size == 3)
            val png = entries.single { it.name.endsWith(".png") }
            check(zip.getInputStream(png).readBytes().contentEquals(byteArrayOf(1, 2, 3, 4)))
            val caption = entries.single { it.name.endsWith(".txt") }
            check(zip.getInputStream(caption).readBytes().decodeToString() == "clean-room cat")
            val manifest = entries.single { it.name.endsWith("manifest.jsonl") }
            kotlinx.serialization.json.Json.parseToJsonElement(
                zip.getInputStream(manifest).readBytes().decodeToString().trim()
            )
            true
        }
    } catch (_: java.util.zip.ZipException) { false }

    @Test
    fun regression_completedRealJvmZipIsAlreadyReadable() = runTest {
        val (atCompleted, afterCollection) = realZipReadability()
        assertTrue(afterCollection, "Real JVM ZIP must be readable after flow collection finishes")
        assertTrue(atCompleted, "Completed must not precede a readable ZIP central directory")
    }

    @Test
    fun control_realJvmZipIsReadableAfterCollection() = runTest {
        assertTrue(realZipReadability().second)
    }
}
