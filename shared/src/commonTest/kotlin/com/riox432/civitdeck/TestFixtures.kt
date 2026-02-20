package com.riox432.civitdeck

import com.riox432.civitdeck.domain.model.Creator
import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.domain.model.ImageStats
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelStats
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.PageMetadata
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SavedPrompt

fun testModel(
    id: Long = 1L,
    name: String = "Test Model",
    type: ModelType = ModelType.Checkpoint,
    nsfw: Boolean = false,
    tags: List<String> = emptyList(),
    creatorName: String = "testuser",
) = Model(
    id = id,
    name = name,
    description = null,
    type = type,
    nsfw = nsfw,
    tags = tags,
    mode = null,
    creator = Creator(username = creatorName, image = null, modelCount = null, link = null),
    stats = ModelStats(
        downloadCount = 100,
        favoriteCount = 50,
        commentCount = 10,
        ratingCount = 20,
        rating = 4.5,
    ),
    modelVersions = listOf(testModelVersion(modelId = id)),
)

fun testModelVersion(
    id: Long = 1L,
    modelId: Long = 1L,
    nsfwLevel: NsfwLevel = NsfwLevel.None,
) = ModelVersion(
    id = id,
    modelId = modelId,
    name = "v1.0",
    description = null,
    createdAt = "2024-01-01",
    baseModel = "SD 1.5",
    trainedWords = emptyList(),
    downloadUrl = "https://example.com/download",
    files = emptyList(),
    images = listOf(testModelImage(nsfwLevel = nsfwLevel)),
    stats = null,
)

fun testModelImage(nsfwLevel: NsfwLevel = NsfwLevel.None) =
    com.riox432.civitdeck.domain.model.ModelImage(
        url = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/test-uuid/original=true/photo.jpeg",
        nsfw = nsfwLevel != NsfwLevel.None,
        nsfwLevel = nsfwLevel,
        width = 512,
        height = 512,
        hash = null,
        meta = null,
    )

fun <T> testPaginatedResult(
    items: List<T> = emptyList(),
    nextCursor: String? = null,
) = PaginatedResult(
    items = items,
    metadata = PageMetadata(nextCursor = nextCursor, nextPage = null),
)

fun testImage(
    id: Long = 1L,
    nsfwLevel: NsfwLevel = NsfwLevel.None,
) = Image(
    id = id,
    url = "https://image.civitai.com/test/$id.jpg",
    hash = null,
    width = 512,
    height = 512,
    nsfw = nsfwLevel != NsfwLevel.None,
    nsfwLevel = nsfwLevel,
    createdAt = "2024-01-01",
    postId = null,
    username = "testuser",
    stats = ImageStats(
        cryCount = 0,
        laughCount = 0,
        likeCount = 10,
        heartCount = 5,
        commentCount = 2,
    ),
    meta = null,
)

fun testFavoriteModelSummary(
    id: Long = 1L,
    name: String = "Fav Model",
    type: ModelType = ModelType.Checkpoint,
) = FavoriteModelSummary(
    id = id,
    name = name,
    type = type,
    nsfw = false,
    thumbnailUrl = null,
    creatorName = "testuser",
    downloadCount = 100,
    favoriteCount = 50,
    rating = 4.5,
    favoritedAt = 1000L,
)

fun testSavedPrompt(
    id: Long = 1L,
    prompt: String = "1girl, white hair",
) = SavedPrompt(
    id = id,
    prompt = prompt,
    negativePrompt = null,
    sampler = "Euler a",
    steps = 20,
    cfgScale = 7.0,
    seed = 12345L,
    modelName = "TestModel",
    size = "512x512",
    sourceImageUrl = null,
    savedAt = 1000L,
)
