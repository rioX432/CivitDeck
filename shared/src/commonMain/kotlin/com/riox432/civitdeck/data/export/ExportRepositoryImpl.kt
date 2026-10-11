package com.riox432.civitdeck.data.export

import com.riox432.civitdeck.domain.model.ExportFormat
import com.riox432.civitdeck.domain.model.ExportProgress
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository
import com.riox432.civitdeck.domain.repository.ExportRepository
import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "ExportRepositoryImpl"

class ExportRepositoryImpl(
    private val datasetRepo: DatasetCollectionRepository,
    private val httpClient: HttpClient,
    private val zipWriterFactory: DatasetZipWriterFactory,
    private val exportPathProvider: ExportPathProvider,
) : ExportRepository {

    override fun exportDataset(datasetId: Long, format: ExportFormat): Flow<ExportProgress> = flow {
        emit(ExportProgress.Preparing)

        // flow firstOrNull() avoids NoSuchElementException if the source flow emits nothing;
        // an empty/absent emission is treated the same as the dataset not being found.
        val collection = datasetRepo.observeCollections().firstOrNull()
            ?.firstOrNull { it.id == datasetId }
            ?: run {
                emit(ExportProgress.Failed("Dataset not found"))
                return@flow
            }

        val allImages = datasetRepo.observeImages(datasetId).firstOrNull().orEmpty()
        val exportable = allImages.filter { it.trainable && !it.excluded }
        val warningCount = allImages.count { !it.trainable || it.excluded }

        if (exportable.isEmpty()) {
            emit(ExportProgress.Failed("No exportable images"))
            return@flow
        }

        val datasetDir = sanitizeName(collection.name)
        val outputDir = exportPathProvider.getExportCacheDirectory()
        val outputPath = "$outputDir/$datasetDir.zip"

        val zipWriter = zipWriterFactory.create(outputPath)
        var closeAttempted = false
        try {
            exportable.forEachIndexed { index, image ->
                emit(ExportProgress.Downloading(index + 1, exportable.size))
                val imageBytes = downloadImageBytes(image.imageUrl)
                val ext = extractExtension(image.imageUrl)
                val baseName = buildEntryName(index + 1, image.caption?.text)

                zipWriter.addEntry("$datasetDir/images/$baseName.$ext", imageBytes)
                zipWriter.addEntry(
                    "$datasetDir/images/$baseName.txt",
                    (image.caption?.text ?: "").encodeToByteArray(),
                )
            }

            emit(ExportProgress.WritingManifest)
            val manifest = buildManifest(exportable, datasetDir)
            zipWriter.addEntry("$datasetDir/manifest.jsonl", manifest.encodeToByteArray())

            closeAttempted = true
            zipWriter.close()
            emit(ExportProgress.Completed(outputPath, warningCount))
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "Export failed for dataset $datasetId: ${e.message}")
            emit(ExportProgress.Failed(e.message ?: "Export failed"))
        } finally {
            if (!closeAttempted) {
                try {
                    zipWriter.close()
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    Logger.e(TAG, "Export cleanup failed for dataset $datasetId: ${e.message}")
                }
            }
        }
    }

    private suspend fun downloadImageBytes(url: String): ByteArray {
        val response = httpClient.get(url)
        check(response.status.isSuccess()) {
            "Image download failed: HTTP ${response.status.value}"
        }
        check(response.contentType()?.match(ContentType.Text.Html) != true) {
            "Expected image bytes but received HTML"
        }
        val imageBytes = response.bodyAsBytes()
        check(imageBytes.isNotEmpty()) { "Downloaded image is empty" }
        return imageBytes
    }
}

private fun sanitizeName(name: String): String =
    name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(MAX_DIR_NAME_LENGTH)

private fun extractExtension(url: String): String {
    val path = url.substringBefore('?')
    val ext = path.substringAfterLast('.', "").lowercase()
    return if (ext in SUPPORTED_EXTENSIONS) ext else "png"
}

private fun buildEntryName(index: Int, captionText: String?): String {
    val prefix = index.toString().padStart(ENTRY_INDEX_PAD, '0')
    if (captionText.isNullOrBlank()) return "${prefix}_image"
    val slug = captionText.take(CAPTION_SLUG_MAX_LENGTH)
        .replace(Regex("[^a-zA-Z0-9 ]"), "")
        .trim()
        .replace(Regex("\\s+"), "_")
        .lowercase()
    return if (slug.isEmpty()) "${prefix}_image" else "${prefix}_$slug"
}

private fun buildManifest(
    images: List<com.riox432.civitdeck.domain.model.DatasetImage>,
    datasetDir: String,
): String = buildString {
    images.forEachIndexed { index, image ->
        val ext = extractExtension(image.imageUrl)
        val baseName = buildEntryName(index + 1, image.caption?.text)
        val manifestEntry = buildJsonObject {
            put("image_path", "$datasetDir/images/$baseName.$ext")
            put("caption", image.caption?.text ?: "")
            put("tags", buildJsonArray { image.tags.forEach { add(it.tag) } })
            put("source", image.sourceType.name)
            put("trainable", image.trainable)
        }
        appendLine(Json.encodeToString(JsonObject.serializer(), manifestEntry))
    }
}

private val SUPPORTED_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")
private const val MAX_DIR_NAME_LENGTH = 64
private const val ENTRY_INDEX_PAD = 3
private const val CAPTION_SLUG_MAX_LENGTH = 40
