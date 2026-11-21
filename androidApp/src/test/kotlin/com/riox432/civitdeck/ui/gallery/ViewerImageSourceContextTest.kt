package com.riox432.civitdeck.ui.gallery

import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.MediaContentType
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.SourceGenerationSnapshot
import com.riox432.civitdeck.domain.model.SourceProvider
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.domain.model.SourceResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ViewerImageSourceContextTest {

    @Test
    fun a_complete_source_image_keeps_its_reference_and_preview_fields() {
        val image = sourceImage(id = 123L, meta = META, reference = reference("123", generation = true))

        val viewer = ViewerImage.fromImage(image)

        assertSame(image, viewer.originalImage)
        assertEquals("123", viewer.originalImage?.sourceReference?.artworkId)
        assertEquals(listOf(SourceResource(versionId = 42L)), viewer.originalImage?.sourceReference?.resources)
        assertEquals(image.url, viewer.url)
        assertEquals(META, viewer.meta)
        assertEquals(MediaContentType.VIDEO, viewer.contentType)
    }

    @Test
    fun a_metadata_free_source_image_keeps_its_reference() {
        val image = sourceImage(id = 456L, meta = null, reference = reference("456", generation = false))

        val viewer = ViewerImage.fromImage(image)

        assertNull(viewer.meta)
        assertSame(image, viewer.originalImage)
        assertEquals(reference("456", generation = false), viewer.originalImage?.sourceReference)
    }

    @Test
    fun an_image_without_a_reference_gets_none_reconstructed_from_its_url() {
        // The URL names an artwork ID, but only the API's own reference may supply one.
        val image = sourceImage(id = 789L, meta = META, reference = null)

        val viewer = ViewerImage.fromImage(image)

        assertSame(image, viewer.originalImage)
        assertNull(viewer.originalImage?.sourceReference)
    }

    @Test
    fun url_only_legacy_viewer_images_carry_no_source() {
        val legacy = ViewerImage(url = "https://comfy.local/view?filename=out.png", meta = META)

        assertNull(legacy.originalImage)
        assertEquals(MediaContentType.IMAGE, legacy.contentType)
    }

    @Test
    fun the_gallery_maps_every_image_through_the_adapter() {
        val images = listOf(
            sourceImage(id = 123L, meta = META, reference = reference("123", generation = true)),
            sourceImage(id = 456L, meta = null, reference = reference("456", generation = false)),
        )

        val viewers = galleryViewerImages(images)

        assertEquals(images.map(ViewerImage::fromImage), viewers)
        assertEquals(images, viewers.map { it.originalImage })
    }

    private fun sourceImage(id: Long, meta: ImageGenerationMeta?, reference: SourceReference?) = Image(
        id = id,
        url = "https://image.civitai.com/x/$id/width=450/$id.mp4",
        hash = null,
        width = 512,
        height = 768,
        nsfw = false,
        nsfwLevel = NsfwLevel.None,
        createdAt = "2026-11-01",
        postId = null,
        username = "creator",
        stats = null,
        meta = meta,
        contentType = MediaContentType.VIDEO,
        sourceReference = reference,
    )

    private fun reference(artworkId: String, generation: Boolean) = SourceReference(
        provider = SourceProvider.Civitai,
        artworkId = artworkId,
        artworkUrl = "https://civitai.com/images/$artworkId",
        creator = "creator",
        generation = if (generation) SourceGenerationSnapshot(prompt = "a lighthouse", steps = 30) else null,
        resources = listOf(SourceResource(versionId = 42L)),
    )

    private companion object {
        val META = ImageGenerationMeta(
            prompt = "a lighthouse",
            negativePrompt = null,
            sampler = "Euler a",
            cfgScale = 7.0,
            steps = 30,
            seed = 1L,
            model = "base",
            size = "512x768",
        )
    }
}
