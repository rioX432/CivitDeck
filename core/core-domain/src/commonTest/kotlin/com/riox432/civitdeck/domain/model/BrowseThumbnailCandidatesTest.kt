package com.riox432.civitdeck.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrowseThumbnailCandidatesTest {

    private fun image(
        url: String,
        level: NsfwLevel = NsfwLevel.None,
        contentType: MediaContentType = MediaContentType.IMAGE,
    ) = ModelImage(
        url = url,
        nsfw = level != NsfwLevel.None,
        nsfwLevel = level,
        width = 512,
        height = 512,
        hash = null,
        meta = null,
        contentType = contentType,
    )

    private fun model(images: List<ModelImage>) = Model(
        id = 1L,
        name = "m",
        description = null,
        type = ModelType.Checkpoint,
        nsfw = false,
        tags = emptyList(),
        mode = null,
        creator = null,
        stats = ModelStats(0, 0, 0, 0, 0.0),
        modelVersions = listOf(
            ModelVersion(
                id = 1L,
                modelId = 1L,
                name = "v1",
                description = null,
                createdAt = "",
                baseModel = null,
                trainedWords = emptyList(),
                downloadUrl = "",
                files = emptyList(),
                images = images,
                stats = null,
            ),
        ),
    )

    @Test
    fun videos_are_never_candidates() {
        val m = model(
            listOf(
                image("video.mp4", contentType = MediaContentType.VIDEO),
                image("photo.jpg"),
            ),
        )
        assertEquals(listOf("photo.jpg"), m.browseThumbnailCandidates().map { it.url })
    }

    @Test
    fun safest_image_comes_first() {
        val m = model(
            listOf(
                image("explicit.jpg", NsfwLevel.X),
                image("mature.jpg", NsfwLevel.Mature),
                image("safe.jpg", NsfwLevel.None),
            ),
        )
        assertEquals(
            listOf("safe.jpg", "mature.jpg", "explicit.jpg"),
            m.browseThumbnailCandidates().map { it.url },
        )
    }

    @Test
    fun original_order_is_kept_within_the_same_level() {
        val m = model(
            listOf(
                image("first.jpg"),
                image("second.jpg"),
            ),
        )
        assertEquals(
            listOf("first.jpg", "second.jpg"),
            m.browseThumbnailCandidates().map { it.url },
        )
    }

    @Test
    fun video_only_models_have_no_candidates() {
        val m = model(listOf(image("clip.mp4", contentType = MediaContentType.VIDEO)))
        assertTrue(m.browseThumbnailCandidates().isEmpty())
    }
}
