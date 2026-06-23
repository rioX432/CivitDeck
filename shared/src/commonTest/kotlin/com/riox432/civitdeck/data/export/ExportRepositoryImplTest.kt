package com.riox432.civitdeck.data.export

import com.riox432.civitdeck.domain.model.Caption
import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ExportFormat
import com.riox432.civitdeck.domain.model.ExportProgress
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.domain.model.ImageTag
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [ExportRepositoryImpl.exportDataset]'s orchestration: the progress-event
 * sequence on success, the failure branches (dataset not found, no exportable images),
 * the trainable/excluded filtering, and that ZIP entries plus a manifest are written.
 */
class ExportRepositoryImplTest {

    /** Fake repo returning a fixed collection list and per-dataset image list. */
    private class FakeDatasetRepo(
        private val collections: List<DatasetCollection>,
        private val images: List<DatasetImage>,
    ) : DatasetCollectionRepository {
        override fun observeCollections(): Flow<List<DatasetCollection>> = flowOf(collections)
        override fun observeImages(datasetId: Long): Flow<List<DatasetImage>> = flowOf(images)
        override suspend fun createCollection(name: String, description: String): Long = 0L
        override suspend fun renameCollection(id: Long, name: String) {}
        override suspend fun deleteCollection(id: Long) {}
        override suspend fun addImage(
            datasetId: Long,
            imageUrl: String,
            sourceType: ImageSource,
            trainable: Boolean,
            tags: List<String>,
        ): Long = 0L
        override suspend fun removeImage(imageId: Long) {}
        override suspend fun removeImages(imageIds: List<Long>) {}
        override suspend fun updateTrainable(imageId: Long, trainable: Boolean) {}
        override suspend fun updateLicenseNote(imageId: Long, licenseNote: String?) {}
        override suspend fun getNonTrainableImages(datasetId: Long): List<DatasetImage> = emptyList()
        override suspend fun updatePHash(imageId: Long, pHash: String?) {}
        override suspend fun markExcluded(imageId: Long, excluded: Boolean) {}
        override suspend fun updateDimensions(imageId: Long, width: Int, height: Int) {}
    }

    /** Records every entry name written and whether [close] was called. */
    private class RecordingZipWriter : DatasetZipWriter {
        val entries = mutableListOf<String>()
        var closed = false
        override fun addEntry(name: String, data: ByteArray) { entries.add(name) }
        override fun close() { closed = true }
    }

    private class RecordingZipWriterFactory(val writer: RecordingZipWriter) : DatasetZipWriterFactory {
        var requestedPath: String? = null
        override fun create(outputPath: String): DatasetZipWriter {
            requestedPath = outputPath
            return writer
        }
    }

    private class FixedPathProvider(private val dir: String) : ExportPathProvider {
        override fun getExportCacheDirectory(): String = dir
    }

    /** HttpClient whose mock engine returns fixed image bytes for any download. */
    private fun bytesClient(): HttpClient {
        val engine = MockEngine {
            respond(content = byteArrayOf(1, 2, 3, 4), status = HttpStatusCode.OK)
        }
        return HttpClient(engine)
    }

    private fun collection(id: Long = 1L, name: String = "My Dataset") =
        DatasetCollection(id = id, name = name, createdAt = 0L, updatedAt = 0L)

    private fun image(
        id: Long,
        url: String = "http://cdn/img$id.png",
        trainable: Boolean = true,
        excluded: Boolean = false,
        caption: String? = "a cat",
        tags: List<String> = listOf("cat"),
    ) = DatasetImage(
        id = id,
        datasetId = 1L,
        imageUrl = url,
        sourceType = ImageSource.CIVITAI,
        trainable = trainable,
        addedAt = 0L,
        tags = tags.mapIndexed { i, t -> ImageTag(id = i.toLong(), datasetImageId = id, tag = t) },
        caption = caption?.let { Caption(datasetImageId = id, text = it) },
        excluded = excluded,
    )

    private fun repo(
        collections: List<DatasetCollection>,
        images: List<DatasetImage>,
        factory: DatasetZipWriterFactory,
    ) = ExportRepositoryImpl(
        datasetRepo = FakeDatasetRepo(collections, images),
        httpClient = bytesClient(),
        zipWriterFactory = factory,
        exportPathProvider = FixedPathProvider("/cache/export"),
    )

    @Test
    fun exportDataset_emits_full_progress_sequence_on_success() = runTest {
        val factory = RecordingZipWriterFactory(RecordingZipWriter())
        val repo = repo(listOf(collection()), listOf(image(1)), factory)

        val events = repo.exportDataset(1L, ExportFormat.ZIP).toList()

        assertEquals(ExportProgress.Preparing, events.first())
        assertTrue(events.any { it is ExportProgress.Downloading })
        assertTrue(events.any { it is ExportProgress.WritingManifest })
        val completed = events.last() as ExportProgress.Completed
        assertEquals("/cache/export/My_Dataset.zip", completed.outputPath)
    }

    @Test
    fun exportDataset_fails_when_dataset_not_found() = runTest {
        val factory = RecordingZipWriterFactory(RecordingZipWriter())
        val repo = repo(emptyList(), emptyList(), factory)

        val events = repo.exportDataset(99L, ExportFormat.ZIP).toList()

        val failed = events.last() as ExportProgress.Failed
        assertEquals("Dataset not found", failed.message)
        // The zip writer is never created when the dataset is missing.
        assertEquals(null, factory.requestedPath)
    }

    @Test
    fun exportDataset_fails_when_no_exportable_images() = runTest {
        val factory = RecordingZipWriterFactory(RecordingZipWriter())
        // Only non-trainable / excluded images present -> nothing to export.
        val repo = repo(
            listOf(collection()),
            listOf(image(1, trainable = false), image(2, excluded = true)),
            factory,
        )

        val events = repo.exportDataset(1L, ExportFormat.ZIP).toList()

        val failed = events.last() as ExportProgress.Failed
        assertEquals("No exportable images", failed.message)
    }

    @Test
    fun exportDataset_writes_image_caption_and_manifest_entries() = runTest {
        val writer = RecordingZipWriter()
        val repo = repo(listOf(collection()), listOf(image(1)), RecordingZipWriterFactory(writer))

        repo.exportDataset(1L, ExportFormat.ZIP).toList()

        // Each image yields an image entry + a sidecar caption .txt, plus one manifest.
        assertTrue(writer.entries.any { it.endsWith(".png") })
        assertTrue(writer.entries.any { it.endsWith(".txt") })
        assertTrue(writer.entries.any { it.endsWith("manifest.jsonl") })
        assertTrue(writer.closed)
    }

    @Test
    fun exportDataset_warning_count_reflects_skipped_images() = runTest {
        val repo = repo(
            listOf(collection()),
            listOf(image(1), image(2, trainable = false), image(3, excluded = true)),
            RecordingZipWriterFactory(RecordingZipWriter()),
        )

        val events = repo.exportDataset(1L, ExportFormat.ZIP).toList()

        val completed = events.last() as ExportProgress.Completed
        // Two images were skipped (one non-trainable, one excluded).
        assertEquals(2, completed.warningCount)
    }
}
