package com.riox432.civitdeck.data.export

import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.domain.model.ExportFormat
import com.riox432.civitdeck.domain.model.ExportProgress
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.io.FilterOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatasetZipTransactionTest : DatasetExportTestFixture() {
    private fun priorZip(target: Path): ByteArray {
        DatasetZipWriterImpl(target.toString()).apply {
            addEntry("previous.txt", byteArrayOf(9, 8, 7))
            close()
        }
        return Files.readAllBytes(target)
    }

    private suspend fun withTarget(action: suspend (Path) -> Unit) {
        val dir = Files.createTempDirectory("civitdeck-transaction-test-")
        try {
            action(dir.resolve("clean_room.zip"))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    private fun assertNoStaging(target: Path) {
        Files.list(target.parent).use { files ->
            assertTrue(files.allMatch { it == target })
        }
    }

    @Test
    fun abortPreservesPreviousBytesAndCannotPublishLater() = runTest {
        withTarget { target ->
            val previous = priorZip(target)
            val writer = DatasetZipWriterImpl(target.toString())
            writer.addEntry("new.txt", byteArrayOf(1))
            assertContentEquals(previous, Files.readAllBytes(target))
            writer.abort()
            writer.abort()
            assertFailsWith<IllegalStateException> { writer.close() }
            assertFailsWith<IllegalStateException> { writer.addEntry("late", byteArrayOf(2)) }
            assertContentEquals(previous, Files.readAllBytes(target))
            assertNoStaging(target)
        }
    }

    @Test
    fun successfulCloseReplacesZipAndAbortCannotDeleteIt() = runTest {
        withTarget { target ->
            priorZip(target)
            val writer = DatasetZipWriterImpl(target.toString())
            writer.addEntry("new.txt", byteArrayOf(1, 2))
            writer.close()
            val committed = Files.readAllBytes(target)
            writer.close()
            writer.abort()
            assertContentEquals(committed, Files.readAllBytes(target))
            ZipFile(target.toFile()).use { zip ->
                assertEquals(listOf("new.txt"), zip.entries().toList().map { it.name })
                assertContentEquals(byteArrayOf(1, 2), zip.getInputStream(zip.getEntry("new.txt")).readBytes())
            }
            assertNoStaging(target)
        }
    }

    @Test
    fun renameFailuresPreservePreviousBytes() = runTest {
        for (unsupported in listOf(false, true)) {
            withTarget { target ->
                val previous = priorZip(target)
                val writer = DatasetZipWriterImpl(
                    target.toString(),
                    { Files.newOutputStream(it) },
                    { from, to ->
                        if (unsupported) throw AtomicMoveNotSupportedException(from.toString(), to.toString(), "test")
                        throw IOException("synthetic rename failure")
                    },
                )
                writer.addEntry("new.txt", byteArrayOf(1))
                assertFailsWith<IOException> { writer.close() }
                writer.abort()
                assertContentEquals(previous, Files.readAllBytes(target))
                assertNoStaging(target)
            }
        }
    }

    @Test
    fun writeAndCloseFailuresPreservePreviousBytes() = runTest {
        for (failWrite in listOf(false, true)) {
            withTarget { target ->
                val previous = priorZip(target)
                var publicationAttempted = false
                var underlyingClosed = false
                val writer = DatasetZipWriterImpl(
                    target.toString(),
                    { path ->
                        object : FilterOutputStream(Files.newOutputStream(path)) {
                            override fun write(value: Int) {
                                if (failWrite) throw IOException("synthetic write failure")
                                out.write(value)
                            }
                            override fun close() {
                                out.close()
                                underlyingClosed = true
                                if (!failWrite) throw IOException("synthetic close failure")
                            }
                        }
                    },
                    { from, to ->
                        publicationAttempted = true
                        Files.move(from, to, StandardCopyOption.ATOMIC_MOVE)
                    },
                )
                assertFailsWith<IOException> {
                    writer.addEntry("new.txt", byteArrayOf(1))
                    writer.close()
                }
                writer.abort()
                assertFalse(publicationAttempted)
                assertTrue(underlyingClosed, "Failure cleanup must close the underlying file handle")
                assertContentEquals(previous, Files.readAllBytes(target))
                assertNoStaging(target)
            }
        }
    }

    private fun repository(
        target: Path,
        client: HttpClient,
        factory: DatasetZipWriterFactory = DatasetZipWriterFactoryImpl(),
    ) = ExportRepositoryImpl(
        FakeDatasetRepo(
            listOf(DatasetCollection(1, "clean_room", createdAt = 0, updatedAt = 0)),
            listOf(image()),
        ),
        client,
        factory,
        object : ExportPathProvider { override fun getExportCacheDirectory() = target.parent.toString() },
    )

    @Test
    fun mediaFailurePreservesPreviousZipAndEmitsNoCompleted() = runTest {
        withTarget { target ->
            val previous = priorZip(target)
            HttpClient(MockEngine { respond("failed", HttpStatusCode.InternalServerError) }).use { client ->
                val events = repository(target, client).exportDataset(1, ExportFormat.ZIP).toList()
                assertTrue(events.last() is ExportProgress.Failed)
                assertTrue(events.none { it is ExportProgress.Completed })
            }
            assertContentEquals(previous, Files.readAllBytes(target))
            assertNoStaging(target)
        }
    }

    @Test
    fun writerConstructionFailureReportsFailedWithoutTouchingPreviousZip() = runTest {
        withTarget { target ->
            val previous = priorZip(target)
            val factory = object : DatasetZipWriterFactory {
                override fun create(outputPath: String): DatasetZipWriter = throw IOException("cannot stage")
            }
            HttpClient(MockEngine { error("must not download") }).use { client ->
                val events = repository(target, client, factory).exportDataset(1, ExportFormat.ZIP).toList()
                assertEquals("cannot stage", (events.last() as ExportProgress.Failed).message)
                assertTrue(events.none { it is ExportProgress.Completed })
            }
            assertContentEquals(previous, Files.readAllBytes(target))
            assertNoStaging(target)
        }
    }

    @Test
    fun cancellationBeforeCommitPreservesPreviousZip() = runTest {
        withTarget { target ->
            val previous = priorZip(target)
            HttpClient(MockEngine { respond(byteArrayOf(1, 2, 3)) }).use { client ->
                assertFailsWith<CancellationException> {
                    repository(target, client).exportDataset(1, ExportFormat.ZIP).collect { event ->
                        if (event is ExportProgress.WritingManifest) throw CancellationException("test cancel")
                    }
                }
            }
            assertContentEquals(previous, Files.readAllBytes(target))
            assertNoStaging(target)
        }
    }

    @Test
    fun cancellationDuringManifestWriteStopsPublication() = runTest {
        withTarget { target ->
            val previous = priorZip(target)
            val events = mutableListOf<ExportProgress>()
            HttpClient(MockEngine { respond(byteArrayOf(1, 2, 3)) }).use { client ->
                val collection = launch {
                    val job = currentCoroutineContext().job
                    val factory = object : DatasetZipWriterFactory {
                        override fun create(outputPath: String): DatasetZipWriter {
                            val writer = DatasetZipWriterImpl(outputPath)
                            return object : DatasetZipWriter by writer {
                                override fun addEntry(name: String, data: ByteArray) {
                                    writer.addEntry(name, data)
                                    if (name.endsWith("manifest.jsonl")) job.cancel()
                                }
                            }
                        }
                    }
                    repository(target, client, factory).exportDataset(1, ExportFormat.ZIP).toList(events)
                }
                collection.join()
                assertTrue(collection.isCancelled)
            }
            assertTrue(events.none { it is ExportProgress.Completed || it is ExportProgress.Failed })
            assertContentEquals(previous, Files.readAllBytes(target))
            assertNoStaging(target)
        }
    }

    @Test
    fun cancellationAfterCommitKeepsCompletedArchive() = runTest {
        withTarget { target ->
            val previous = priorZip(target)
            HttpClient(MockEngine { respond(byteArrayOf(1, 2, 3)) }).use { client ->
                assertFailsWith<CancellationException> {
                    repository(target, client).exportDataset(1, ExportFormat.ZIP).collect { event ->
                        if (event is ExportProgress.Completed) throw CancellationException("test cancel")
                    }
                }
            }
            assertFalse(previous.contentEquals(Files.readAllBytes(target)))
            ZipFile(target.toFile()).use { assertEquals(3, it.size()) }
            assertNoStaging(target)
        }
    }

    @Test
    fun abortWithoutPreviousZipCreatesNoOutput() = runTest {
        withTarget { target ->
            DatasetZipWriterImpl(target.toString()).apply {
                addEntry("new", byteArrayOf(1))
                abort()
            }
            assertFalse(Files.exists(target))
            assertNoStaging(target)
        }
    }

    @Test
    fun overlappingWritersPublishIndependentlyAndLastSuccessWins() = runTest {
        withTarget { target ->
            priorZip(target)
            val first = DatasetZipWriterImpl(target.toString())
            val second = DatasetZipWriterImpl(target.toString())
            val aborted = DatasetZipWriterImpl(target.toString())
            first.addEntry("first", byteArrayOf(1))
            second.addEntry("second", byteArrayOf(2))
            aborted.addEntry("aborted", byteArrayOf(3))
            first.close()
            val firstBytes = Files.readAllBytes(target)
            aborted.abort()
            assertContentEquals(firstBytes, Files.readAllBytes(target))
            second.close()
            first.abort()
            ZipFile(target.toFile()).use { zip ->
                assertEquals(listOf("second"), zip.entries().toList().map { it.name })
            }
            assertNoStaging(target)
        }
    }

    @Test
    fun streamCreationFailureCleansStagingAndPreservesPreviousZip() = runTest {
        withTarget { target ->
            val previous = priorZip(target)
            assertFailsWith<IOException> {
                DatasetZipWriterImpl(
                    target.toString(),
                    { throw IOException("cannot open") },
                    { _, _ -> error("unused") }
                )
            }
            assertContentEquals(previous, Files.readAllBytes(target))
            assertNoStaging(target)
        }
    }
}
