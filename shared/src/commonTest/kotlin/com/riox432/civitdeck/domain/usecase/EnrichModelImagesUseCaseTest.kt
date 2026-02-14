package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.ModelRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EnrichModelImagesUseCaseTest {

    private val sampleMeta = ImageGenerationMeta(
        prompt = "1girl, white hair",
        negativePrompt = "bad quality",
        sampler = "Euler a",
        cfgScale = 7.0,
        steps = 20,
        seed = 12345L,
        model = "TestModel",
        size = "512x512",
    )

    private fun image(uuid: String, meta: ImageGenerationMeta? = null) = ModelImage(
        url = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/$uuid/original=true/photo.jpeg",
        nsfw = false,
        nsfwLevel = NsfwLevel.None,
        width = 512,
        height = 512,
        hash = null,
        meta = meta,
    )

    private fun fakeRepository(versionImages: List<ModelImage>) = object : ModelRepository {
        override suspend fun getModelVersion(id: Long) = ModelVersion(
            id = id,
            modelId = 1L,
            name = "v1",
            description = null,
            createdAt = "",
            baseModel = null,
            trainedWords = emptyList(),
            downloadUrl = "",
            files = emptyList(),
            images = versionImages,
            stats = null,
        )

        override suspend fun getModels(
            query: String?,
            tag: String?,
            type: ModelType?,
            sort: SortOrder?,
            period: TimePeriod?,
            baseModels: List<BaseModel>?,
            cursor: String?,
            limit: Int?,
            username: String?,
            nsfw: Boolean?,
        ): PaginatedResult<Model> = error("not used")

        override suspend fun getModel(id: Long): Model = error("not used")
        override suspend fun getModelVersionByHash(hash: String): ModelVersion = error("not used")
    }

    @Test
    fun enriches_images_with_matching_uuid() = runTest {
        val uuid = "abc-123-def"
        val enrichedImage = image(uuid, meta = sampleMeta)
        val repo = fakeRepository(listOf(enrichedImage))
        val useCase = EnrichModelImagesUseCase(repo)

        val input = listOf(image(uuid, meta = null))
        val result = useCase(modelVersionId = 1L, images = input)

        assertEquals(1, result.size)
        assertNotNull(result[0].meta)
        assertEquals("1girl, white hair", result[0].meta?.prompt)
    }

    @Test
    fun preserves_existing_meta() = runTest {
        val uuid = "abc-123-def"
        val existingMeta = sampleMeta.copy(prompt = "original prompt")
        val repo = fakeRepository(listOf(image(uuid, meta = sampleMeta)))
        val useCase = EnrichModelImagesUseCase(repo)

        val input = listOf(image(uuid, meta = existingMeta))
        val result = useCase(modelVersionId = 1L, images = input)

        assertEquals("original prompt", result[0].meta?.prompt)
    }

    @Test
    fun leaves_meta_null_when_no_match() = runTest {
        val repo = fakeRepository(listOf(image("uuid-a", meta = sampleMeta)))
        val useCase = EnrichModelImagesUseCase(repo)

        val input = listOf(image("uuid-b", meta = null))
        val result = useCase(modelVersionId = 1L, images = input)

        assertNull(result[0].meta)
    }

    @Test
    fun handles_multiple_images() = runTest {
        val enrichedImages = listOf(
            image("uuid-1", meta = sampleMeta.copy(prompt = "prompt1")),
            image("uuid-2", meta = sampleMeta.copy(prompt = "prompt2")),
        )
        val repo = fakeRepository(enrichedImages)
        val useCase = EnrichModelImagesUseCase(repo)

        val input = listOf(
            image("uuid-1", meta = null),
            image("uuid-2", meta = null),
            image("uuid-3", meta = null),
        )
        val result = useCase(modelVersionId = 1L, images = input)

        assertEquals("prompt1", result[0].meta?.prompt)
        assertEquals("prompt2", result[1].meta?.prompt)
        assertNull(result[2].meta)
    }
}
