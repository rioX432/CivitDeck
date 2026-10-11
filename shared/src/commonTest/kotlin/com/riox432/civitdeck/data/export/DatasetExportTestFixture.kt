package com.riox432.civitdeck.data.export

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.data.api.RetryConfig
import com.riox432.civitdeck.data.api.createHttpClient
import com.riox432.civitdeck.domain.model.Caption
import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.domain.model.ImageTag
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

abstract class DatasetExportTestFixture {

    protected class FakeDatasetRepo(
        private val collections: List<DatasetCollection>,
        private val images: List<DatasetImage>,
    ) : DatasetCollectionRepository {
        override fun observeCollections(): Flow<List<DatasetCollection>> = flowOf(collections)
        override fun observeImages(datasetId: Long): Flow<List<DatasetImage>> = flowOf(images)
        override suspend fun createCollection(name: String, description: String): Long = 0L
        override suspend fun renameCollection(id: Long, name: String) = Unit
        override suspend fun deleteCollection(id: Long) = Unit
        override suspend fun addImage(
            datasetId: Long,
            imageUrl: String,
            sourceType: ImageSource,
            trainable: Boolean,
            tags: List<String>,
        ): Long = 0L
        override suspend fun removeImage(imageId: Long) = Unit
        override suspend fun removeImages(imageIds: List<Long>) = Unit
        override suspend fun updateTrainable(imageId: Long, trainable: Boolean) = Unit
        override suspend fun updateLicenseNote(imageId: Long, licenseNote: String?) = Unit
        override suspend fun getNonTrainableImages(datasetId: Long): List<DatasetImage> = emptyList()
        override suspend fun updatePHash(imageId: Long, pHash: String?) = Unit
        override suspend fun markExcluded(imageId: Long, excluded: Boolean) = Unit
        override suspend fun updateDimensions(imageId: Long, width: Int, height: Int) = Unit
    }

    protected val sentinel = "civitdeck-test-sentinel"
    protected data class Seen(val url: String, val authorization: String?)
    protected class Writer(val failClose: Boolean = false) : DatasetZipWriter {
        val entries = linkedMapOf<String, ByteArray>()
        var closed = false
        override fun addEntry(name: String, data: ByteArray) { entries[name] = data }
        override fun close() {
            if (failClose) error("synthetic-close-failure")
            closed = true
        }
    }
    protected fun client(provider: ApiKeyProvider, seen: MutableList<Seen>): HttpClient =
        createHttpClient(
            MockEngine { request ->
                check(
                    request.url.host in setOf(
                        "media.example.test", "comfy.example.test", "civitai.com", "civitai.com.example.test",
                    )
                )
                seen += Seen(request.url.toString(), request.headers[HttpHeaders.Authorization])
                respond(byteArrayOf(1, 2, 3, 4), HttpStatusCode.OK)
            },
            provider,
            retryConfig = RetryConfig(maxRetries = 0)
        )

    protected fun image(
        id: Long = 1,
        caption: String = "clean-room cat",
        trainable: Boolean = true,
        excluded: Boolean = false,
    ) = DatasetImage(
        id = id,
        datasetId = 1,
        imageUrl = "https://comfy.example.test/view?filename=sample.png",
        sourceType = ImageSource.GENERATED,
        trainable = trainable, excluded = excluded, addedAt = 0,
        caption = Caption(id, caption), tags = listOf(ImageTag(id, id, "synthetic")),
    )
    protected fun repo(
        client: HttpClient,
        writer: Writer,
        images: List<DatasetImage> = listOf(image())
    ) = ExportRepositoryImpl(
        FakeDatasetRepo(listOf(DatasetCollection(1, "clean_room", createdAt = 0, updatedAt = 0)), images),
        client,
        object : DatasetZipWriterFactory { override fun create(outputPath: String) = writer },
        object : ExportPathProvider { override fun getExportCacheDirectory() = "/synthetic/no-file-written" },
    )
}
