package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.CapabilityProblem
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.ComfyUIJobTarget
import com.riox432.civitdeck.domain.model.ComparisonKind
import com.riox432.civitdeck.domain.model.DiffusionModelResources
import com.riox432.civitdeck.domain.model.EffectiveGenerationSettings
import com.riox432.civitdeck.domain.model.GenerationProgress
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.LoraSelection
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelFile
import com.riox432.civitdeck.domain.model.ModelLicenseInfo
import com.riox432.civitdeck.domain.model.ModelSearchQuery
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.PreparedReferenceGeneration
import com.riox432.civitdeck.domain.model.ReferenceResourceReview
import com.riox432.civitdeck.domain.model.ReferenceSetting
import com.riox432.civitdeck.domain.model.ResourceConfidence
import com.riox432.civitdeck.domain.model.SourceGenerationSnapshot
import com.riox432.civitdeck.domain.model.SourceProvider
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.domain.model.SourceResource
import com.riox432.civitdeck.domain.model.TargetCapability
import com.riox432.civitdeck.domain.model.TargetFileHash
import com.riox432.civitdeck.domain.repository.ComfyUIGenerationRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReviewReferenceResourcesUseCaseTest {

    private val targetA = ComfyUIJobTarget(1, "a", 8188, false, false, null)
    private val targetB = ComfyUIJobTarget(2, "b", 9000, false, false, null)

    private val generationRepo = FakeGenerationRepo()
    private val versions = mutableMapOf<Long, ModelVersion>()
    private val useCase = ReviewReferenceResourcesUseCase(
        generationRepo,
        FakeModelRepository(versions),
        ExtractWorkflowParametersUseCase(ParseAppModeMetadataUseCase()),
    )

    private fun version(id: Long, vararg files: Pair<String, String?>) {
        versions[id] = ModelVersion(
            id = id, modelId = id, name = "v$id", description = null, createdAt = "", baseModel = null,
            trainedWords = emptyList(), downloadUrl = "", images = emptyList(), stats = null,
            files = files.mapIndexed { i, (name, sha) ->
                ModelFile(
                    id = i.toLong(), name = name, sizeKB = 1.0, type = null, format = null, fp = null, size = null,
                    downloadUrl = "", primary = i == 0, hashes = sha?.let { mapOf("SHA256" to it) }.orEmpty(),
                )
            },
        )
    }

    private fun source(
        generation: SourceGenerationSnapshot? = null,
        vararg resources: SourceResource,
    ) = SourceReference(
        provider = SourceProvider.Civitai,
        artworkId = "1",
        artworkUrl = SourceReference.civitaiArtworkUrl(1),
        generation = generation,
        resources = resources.toList(),
    )

    private fun prepared(
        checkpoint: String = CKPT,
        loras: List<LoraSelection> = emptyList(),
        target: ComfyUIJobTarget = targetA,
    ): PreparedReferenceGeneration {
        val params = ComfyUIGenerationParams(
            checkpoint = checkpoint, prompt = "a cat", negativePrompt = "", steps = 20, cfgScale = 7.0,
            seed = 42, width = 512, height = 768, samplerName = "euler", scheduler = "normal",
            loraSelections = loras,
        )
        return PreparedReferenceGeneration(1, target, params, graph(checkpoint, loras), EffectiveGenerationSettings.from(params))
    }

    private suspend fun review(
        source: SourceReference,
        prepared: PreparedReferenceGeneration = prepared(),
        hashes: List<TargetFileHash> = emptyList(),
        owned: Set<String> = emptySet(),
    ): ReferenceResourceReview = useCase(prepared, source, hashes, owned)

    // --- Resource confidence ---

    private data class ConfidenceCase(
        val name: String,
        val targetCheckpoints: List<String>,
        val hashes: List<TargetFileHash>,
        val owned: Set<String>,
        val expected: ResourceConfidence,
        val versionFiles: Array<Pair<String, String?>> = arrayOf("model.safetensors" to "AA11"),
    )

    @Test
    fun confidence_states_follow_the_evidence_bound_to_the_target() = runTest {
        val cases = listOf(
            ConfidenceCase(
                "hash recorded for this target and file matches",
                listOf("sdxl/model.safetensors"), listOf(TargetFileHash(targetA, "sdxl/model.safetensors", "aa11")),
                emptySet(), ResourceConfidence.VERIFIED,
            ),
            ConfidenceCase(
                "name match only", listOf("sdxl/model.safetensors"), emptyList(), emptySet(),
                ResourceConfidence.FILENAME_ONLY,
            ),
            ConfidenceCase(
                "phone owns the hash", listOf("model.safetensors"), emptyList(), setOf("AA11"),
                ResourceConfidence.FILENAME_ONLY,
            ),
            ConfidenceCase(
                "matching hash recorded for another target",
                listOf("model.safetensors"), listOf(TargetFileHash(targetB, "model.safetensors", "aa11")),
                emptySet(), ResourceConfidence.FILENAME_ONLY,
            ),
            ConfidenceCase(
                "hash recorded for this target differs",
                listOf("model.safetensors"), listOf(TargetFileHash(targetA, "model.safetensors", "bb22")),
                emptySet(), ResourceConfidence.MISSING,
            ),
            ConfidenceCase("no file with the name", listOf("other.safetensors"), emptyList(), emptySet(), ResourceConfidence.MISSING),
            ConfidenceCase(
                "version lists no files", listOf("model.safetensors"), emptyList(), emptySet(),
                ResourceConfidence.UNKNOWN, versionFiles = emptyArray(),
            ),
        )
        for (case in cases) {
            version(10, *case.versionFiles)
            generationRepo.objectInfo[targetA] = objectInfo(checkpoints = case.targetCheckpoints + CKPT)
            val resource = review(source(null, SourceResource(10, "checkpoint")), hashes = case.hashes, owned = case.owned)
                .resources.single()
            assertEquals(case.expected, resource.confidence, case.name)
        }
    }

    @Test
    fun local_ownership_is_reported_apart_from_target_identity() = runTest {
        version(10, "model.safetensors" to "AA11")
        generationRepo.objectInfo[targetA] = objectInfo(checkpoints = listOf(CKPT, "model.safetensors"))
        val resource = review(source(null, SourceResource(10, "checkpoint")), owned = setOf("aa11")).resources.single()
        assertTrue(resource.ownedLocally)
        assertEquals(ResourceConfidence.FILENAME_ONLY, resource.confidence)
    }

    @Test
    fun a_failed_version_lookup_is_unknown() = runTest {
        generationRepo.objectInfo[targetA] = objectInfo()
        val resource = review(source(null, SourceResource(99, "lora"))).resources.single()
        assertEquals(ResourceConfidence.UNKNOWN, resource.confidence)
    }

    // --- Target capability ---

    @Test
    fun only_the_prepared_target_object_info_is_read() = runTest {
        // B has the checkpoint, and the legacy global cache would answer with B's choices.
        generationRepo.objectInfo[targetA] = objectInfo(checkpoints = listOf("a_only.safetensors"))
        generationRepo.objectInfo[targetB] = objectInfo()
        generationRepo.legacyObjectInfo = objectInfo()

        val review = review(source(), prepared(target = targetA))

        assertEquals(listOf(targetA), generationRepo.fetchedTargets)
        assertFalse(generationRepo.legacyCalled)
        assertEquals(
            TargetCapability.Blocked(listOf(CapabilityProblem.UnsupportedChoice("CheckpointLoaderSimple", "ckpt_name", CKPT))),
            review.capability,
        )
        assertFalse(review.canConfirm)
    }

    @Test
    fun unreadable_target_object_info_blocks_confirmation() = runTest {
        version(10, "model.safetensors" to "AA11")
        for (body in listOf(null, "<html>proxy error</html>")) {
            generationRepo.objectInfo.clear()
            body?.let { generationRepo.objectInfo[targetA] = it }

            val review = review(source(null, SourceResource(10, "checkpoint")))

            assertIs<TargetCapability.Unavailable>(review.capability, "body=$body")
            assertFalse(review.canConfirm)
            assertEquals(ResourceConfidence.UNKNOWN, review.resources.single().confidence)
        }
    }

    @Test
    fun missing_nodes_and_files_block_with_their_names() = runTest {
        generationRepo.objectInfo[targetA] = objectInfo(loras = emptyList(), withKSampler = false)

        val review = review(source(), prepared(loras = listOf(LoraSelection("style.safetensors", 0.8f, 0.8f))))

        val problems = assertIs<TargetCapability.Blocked>(review.capability).problems
        assertTrue(CapabilityProblem.MissingNode("KSampler") in problems, "$problems")
        assertTrue(CapabilityProblem.UnsupportedChoice("LoraLoader", "lora_name", "style.safetensors") in problems)
    }

    @Test
    fun v3_combo_scheduler_choices_are_checked() = runTest {
        generationRepo.objectInfo[targetA] = objectInfo(schedulers = listOf("karras"))
        val problems = assertIs<TargetCapability.Blocked>(review(source()).capability).problems
        assertEquals(listOf(CapabilityProblem.UnsupportedChoice("KSampler", "scheduler", "normal")), problems)
    }

    // --- Settings ---

    @Test
    fun incomplete_metadata_is_unavailable_and_needs_acknowledgement() = runTest {
        generationRepo.objectInfo[targetA] = objectInfo()

        val review = review(source())

        assertEquals(TargetCapability.Supported, review.capability)
        assertTrue(review.canConfirm)
        assertTrue(review.settings.all { it.kind == ComparisonKind.SOURCE_UNAVAILABLE }, "${review.settings}")
        assertTrue(review.requiresVariationAcknowledgement)
    }

    @Test
    fun recorded_settings_are_compared_with_the_effective_ones() = runTest {
        version(10, "model.safetensors" to "AA11")
        version(20, "style.safetensors" to "CC33")
        generationRepo.objectInfo[targetA] = objectInfo(checkpoints = listOf("model.safetensors"))
        val generation = SourceGenerationSnapshot(
            prompt = "a cat", sampler = "Euler", cfgScale = 7.0, steps = 30, seed = 42, model = "model", size = "512x768",
        )

        val review = review(
            source(generation, SourceResource(10, "checkpoint"), SourceResource(20, "lora", weight = 0.8)),
            prepared(checkpoint = "model.safetensors", loras = listOf(LoraSelection("style.safetensors", 0.8f, 0.8f))),
        )

        val kinds = review.settings.associate { it.setting to it.kind }
        assertEquals(ComparisonKind.MATCH, kinds[ReferenceSetting.PROMPT])
        assertEquals(ComparisonKind.MATCH, kinds[ReferenceSetting.MODEL])
        assertEquals(ComparisonKind.MATCH, kinds[ReferenceSetting.DIMENSIONS])
        assertEquals(ComparisonKind.VARIATION, kinds[ReferenceSetting.STEPS])
        assertEquals(ComparisonKind.MATCH, kinds[ReferenceSetting.CFG])
        assertEquals(ComparisonKind.MATCH, kinds[ReferenceSetting.SAMPLER])
        assertEquals(ComparisonKind.SOURCE_UNAVAILABLE, kinds[ReferenceSetting.SCHEDULER])
        assertEquals(ComparisonKind.MATCH, kinds[ReferenceSetting.SEED])
        assertEquals(ComparisonKind.MATCH, kinds[ReferenceSetting.LORA_WEIGHT])
        assertTrue(review.resources.all { it.selected })
        assertTrue(review.requiresVariationAcknowledgement)
    }

    @Test
    fun a_fallback_checkpoint_and_a_dropped_lora_are_variations() = runTest {
        version(10, "model.safetensors" to "AA11")
        version(20, "style.safetensors" to "CC33")
        generationRepo.objectInfo[targetA] = objectInfo()

        val review = review(
            source(SourceGenerationSnapshot(model = "model"), SourceResource(10, "checkpoint"), SourceResource(20, "lora", 0.6)),
        )

        val model = review.settings.single { it.setting == ReferenceSetting.MODEL }
        assertEquals(ComparisonKind.VARIATION, model.kind)
        assertEquals(CKPT, model.effective)
        val lora = review.settings.single { it.setting == ReferenceSetting.LORA_WEIGHT }
        assertEquals(ComparisonKind.VARIATION, lora.kind)
        assertEquals(null, lora.effective)
    }

    // --- Fixtures ---

    private class FakeGenerationRepo : ComfyUIGenerationRepository {
        val objectInfo = mutableMapOf<ComfyUIJobTarget, String>()
        val fetchedTargets = mutableListOf<ComfyUIJobTarget>()
        var legacyObjectInfo: String? = null
        var legacyCalled = false

        override suspend fun fetchObjectInfo(target: ComfyUIJobTarget): String {
            fetchedTargets += target
            return objectInfo[target] ?: error("connection refused")
        }

        override suspend fun fetchObjectInfo(): String {
            legacyCalled = true
            return legacyObjectInfo ?: error("not used")
        }

        override suspend fun fetchCheckpoints(): List<String> = error("not used")
        override suspend fun fetchLoras(): List<String> = error("not used")
        override suspend fun fetchControlNets(): List<String> = error("not used")
        override suspend fun fetchDiffusionModelResources(): DiffusionModelResources = error("not used")
        override suspend fun submitGeneration(params: ComfyUIGenerationParams): String = error("not used")
        override suspend fun pollGenerationResult(promptId: String): GenerationResult = error("not used")
        override fun observeGenerationProgress(promptId: String, host: String, port: Int): Flow<GenerationProgress> =
            emptyFlow()
        override fun observeGenerationProgress(
            promptId: String,
            baseUrl: String,
            wsScheme: String,
        ): Flow<GenerationProgress> = emptyFlow()
        override fun getImageUrl(filename: String, subfolder: String, type: String): String = ""
        override suspend fun interruptGeneration() = Unit
        override suspend fun uploadMaskImage(maskPngBytes: ByteArray): String = error("not used")
    }

    private class FakeModelRepository(private val versions: Map<Long, ModelVersion>) : ModelRepository {
        override suspend fun getModelVersion(id: Long): ModelVersion = versions[id] ?: error("404")
        override suspend fun getModels(query: ModelSearchQuery): PaginatedResult<Model> = error("not used")
        override suspend fun getModel(id: Long): Model = error("not used")
        override suspend fun getModelVersionByHash(hash: String): ModelVersion = error("not used")
        override suspend fun getModelLicense(versionId: Long): ModelLicenseInfo? = null
    }

    private companion object {
        const val CKPT = "base.safetensors"

        fun combo(values: List<String>) = values.joinToString(prefix = "[[", postfix = "]]") { "\"$it\"" }

        /** The built-in text-to-image graph's node classes, as the prepared workflow uses them. */
        fun objectInfo(
            checkpoints: List<String> = listOf(CKPT),
            loras: List<String> = listOf("style.safetensors"),
            schedulers: List<String> = listOf("normal", "karras"),
            withKSampler: Boolean = true,
        ): String {
            val ksampler = """
                "KSampler": {"input": {"required": {
                  "model": ["MODEL"], "seed": ["INT", {"min": 0}], "steps": ["INT"], "cfg": ["FLOAT"],
                  "sampler_name": [["euler", "dpmpp_2m"]],
                  "scheduler": ["COMBO", {"options": [${schedulers.joinToString { "\"$it\"" }}]}],
                  "positive": ["CONDITIONING"], "negative": ["CONDITIONING"], "latent_image": ["LATENT"],
                  "denoise": ["FLOAT"]}}},
            """.trimIndent().takeIf { withKSampler }.orEmpty()
            return """
                {
                  "CheckpointLoaderSimple": {"input": {"required": {"ckpt_name": ${combo(checkpoints)}}}},
                  "LoraLoader": {"input": {"required": {"model": ["MODEL"], "clip": ["CLIP"],
                    "lora_name": ${combo(loras)}, "strength_model": ["FLOAT"], "strength_clip": ["FLOAT"]}}},
                  "CLIPTextEncode": {"input": {"required": {"text": ["STRING", {"multiline": true}], "clip": ["CLIP"]}}},
                  "EmptyLatentImage": {"input": {"required": {"width": ["INT"], "height": ["INT"], "batch_size": ["INT"]}}},
                  $ksampler
                  "VAEDecode": {"input": {"required": {"samples": ["LATENT"], "vae": ["VAE"]}}},
                  "SaveImage": {"input": {"required": {"images": ["IMAGE"], "filename_prefix": ["STRING"]}}}
                }
            """.trimIndent()
        }

        fun graph(checkpoint: String, loras: List<LoraSelection>): String {
            val loraNodes = loras.mapIndexed { i, lora ->
                """"${100 + i}": {"class_type": "LoraLoader", "inputs": {"model": ["3", 0], "clip": ["3", 1],
                  "lora_name": "${lora.name}", "strength_model": ${lora.strengthModel}, "strength_clip": ${lora.strengthClip}}},"""
            }.joinToString("")
            return """
                {
                  "3": {"class_type": "CheckpointLoaderSimple", "inputs": {"ckpt_name": "$checkpoint"}},
                  $loraNodes
                  "6": {"class_type": "CLIPTextEncode", "inputs": {"text": "a cat", "clip": ["3", 1]}},
                  "7": {"class_type": "CLIPTextEncode", "inputs": {"text": "", "clip": ["3", 1]}},
                  "5": {"class_type": "EmptyLatentImage", "inputs": {"width": 512, "height": 768, "batch_size": 1}},
                  "4": {"class_type": "KSampler", "inputs": {"model": ["3", 0], "seed": 42, "steps": 20, "cfg": 7.0,
                    "sampler_name": "euler", "scheduler": "normal", "positive": ["6", 0], "negative": ["7", 0],
                    "latent_image": ["5", 0], "denoise": 1.0}},
                  "8": {"class_type": "VAEDecode", "inputs": {"samples": ["4", 0], "vae": ["3", 2]}},
                  "9": {"class_type": "SaveImage", "inputs": {"images": ["8", 0], "filename_prefix": "CivitDeck"}}
                }
            """.trimIndent()
        }
    }
}
