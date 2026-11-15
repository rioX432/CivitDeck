package com.riox432.civitdeck.data.api

import com.riox432.civitdeck.data.api.huggingface.HuggingFaceModelDto
import com.riox432.civitdeck.data.api.huggingface.toDomain
import com.riox432.civitdeck.data.api.tensorart.TensorArtModelDto
import com.riox432.civitdeck.data.api.tensorart.toDomain
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelReference
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.ModelStats
import com.riox432.civitdeck.domain.model.ModelType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProviderModelReferenceTest {

    @Test
    fun civitaiConstructorWithoutReferenceDefaultsToExactCivitaiReference() {
        val model = model(id = 4201)

        assertEquals(ModelSource.CIVITAI, model.reference.source)
        assertEquals("4201", model.reference.nativeId)
        assertEquals(4201L, model.reference.nativeDetailId)
        assertEquals("https://civitai.com/models/4201", model.reference.canonicalUrl)
        assertEquals("CIVITAI:4201", model.reference.stableKey)
        assertTrue(model.nativeActionsSupported)
    }

    @Test
    fun legacyExternalModelHasNoIdentifierDetailIdOrGuessedUrl() {
        listOf(ModelSource.HUGGING_FACE, ModelSource.TENSOR_ART).forEach { source ->
            val model = model(id = 4201, source = source)

            assertNull(model.reference.nativeId)
            assertNull(model.reference.stableKey)
            assertNull(model.reference.nativeDetailId)
            assertNull(model.reference.canonicalUrl)
            assertFalse(model.nativeActionsSupported)
        }
    }

    @Test
    fun threeProvidersSharingOneNumericIdHaveDistinctKeysAndOnlyCivitaiHasDetailId() {
        val civitai = model(id = 12345)
        val tensorArt = TensorArtModelDto(id = "12345", name = "ta").toDomain()
        val huggingFace = model(
            id = 12345,
            source = ModelSource.HUGGING_FACE,
            reference = ModelReference(
                source = ModelSource.HUGGING_FACE,
                nativeId = "owner/repo",
                canonicalUrl = "https://huggingface.co/owner/repo",
            ),
        )
        val models = listOf(civitai, tensorArt, huggingFace)

        assertEquals(setOf(12345L), models.map { it.id }.toSet())
        assertEquals(3, models.map { it.reference.stableKey }.toSet().size)
        assertEquals(12345L, civitai.reference.nativeDetailId)
        assertNull(tensorArt.reference.nativeDetailId)
        assertNull(huggingFace.reference.nativeDetailId)
        assertFalse(tensorArt.nativeActionsSupported)
        assertFalse(huggingFace.nativeActionsSupported)
    }

    @Test
    fun huggingFaceIdsWithSameStringHashRemainDistinctByNativeId() {
        // "Aa" and "BB" share String.hashCode (2112), so their numeric IDs collide.
        val first = HuggingFaceModelDto(modelId = "owner/Aa").toDomain()
        val second = HuggingFaceModelDto(modelId = "owner/BB").toDomain()

        assertEquals(first.id, second.id)
        assertEquals("owner/Aa", first.reference.nativeId)
        assertEquals("owner/BB", second.reference.nativeId)
        assertNotEquals(first.reference.stableKey, second.reference.stableKey)
    }

    @Test
    fun huggingFaceMapperKeepsFullRepositoryIdAndEncodedCanonicalUrl() {
        val model = HuggingFaceModelDto(modelId = "my org/model?v#1").toDomain()

        assertEquals("my org/model?v#1", model.reference.nativeId)
        assertEquals("https://huggingface.co/my%20org/model%3Fv%231", model.reference.canonicalUrl)
        assertNull(model.reference.nativeDetailId)
    }

    @Test
    fun huggingFaceUnsafePathGetsNoUrlButKeepsExactId() {
        listOf("../etc", "owner//repo", "a/b/c").forEach { modelId ->
            val reference = HuggingFaceModelDto(modelId = modelId).toDomain().reference

            assertEquals(modelId, reference.nativeId)
            assertNull(reference.canonicalUrl)
        }
    }

    @Test
    fun huggingFaceMissingIdIsLegacyReference() {
        val reference = HuggingFaceModelDto(modelId = "").toDomain().reference

        assertNull(reference.nativeId)
        assertNull(reference.canonicalUrl)
        assertNull(reference.nativeDetailId)
    }

    @Test
    fun tensorArtMapperKeepsOriginalIdAndEncodedCanonicalUrl() {
        val numeric = TensorArtModelDto(id = "619225630271212879", name = "n").toDomain()
        val textual = TensorArtModelDto(id = "abc/def", name = "t").toDomain()

        assertEquals("619225630271212879", numeric.reference.nativeId)
        assertEquals("https://tensor.art/models/619225630271212879", numeric.reference.canonicalUrl)
        assertEquals("abc/def", textual.reference.nativeId)
        assertEquals("https://tensor.art/models/abc%2Fdef", textual.reference.canonicalUrl)
        assertNull(textual.reference.nativeDetailId)
    }

    @Test
    fun referenceRejectsNonHttpsUrlAndMismatchedSource() {
        assertFailsWith<IllegalArgumentException> {
            ModelReference(ModelSource.TENSOR_ART, "1", "http://tensor.art/models/1")
        }
        assertFailsWith<IllegalArgumentException> {
            model(id = 1, reference = ModelReference.civitai(1).copy(source = ModelSource.TENSOR_ART))
        }
    }

    private fun model(
        id: Long,
        source: ModelSource = ModelSource.CIVITAI,
        reference: ModelReference? = null,
    ): Model {
        val base = Model(
            id = id,
            name = "m",
            description = null,
            type = ModelType.Checkpoint,
            nsfw = false,
            tags = emptyList(),
            mode = null,
            creator = null,
            stats = ModelStats(0, 0, 0, 0, 0.0),
            modelVersions = emptyList(),
            source = source,
        )
        return if (reference == null) base else base.copy(reference = reference)
    }
}
