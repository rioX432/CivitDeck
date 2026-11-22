package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.CapabilityProblem
import com.riox432.civitdeck.domain.model.ComfyUIJobTarget
import com.riox432.civitdeck.domain.model.ComparisonKind
import com.riox432.civitdeck.domain.model.EffectiveGenerationSettings
import com.riox432.civitdeck.domain.model.PreparedReferenceGeneration
import com.riox432.civitdeck.domain.model.ReferenceResourceReview
import com.riox432.civitdeck.domain.model.ReferenceSetting
import com.riox432.civitdeck.domain.model.ResourceConfidence
import com.riox432.civitdeck.domain.model.ResourceReview
import com.riox432.civitdeck.domain.model.SettingComparison
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.domain.model.SourceResource
import com.riox432.civitdeck.domain.model.TargetCapability
import com.riox432.civitdeck.domain.model.TargetFileHash
import com.riox432.civitdeck.domain.repository.ComfyUIGenerationRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reviews a prepared reference generation against the server it will be sent to.
 *
 * object_info is read fresh from the prepared target, never through [FetchObjectInfoUseCase]:
 * its single cache key can hold another server's choices. A target file only ever counts as
 * the recorded version by file name unless a SHA-256 bound to that same target and file
 * matches; a hash the phone owns says nothing about the target.
 */
class ReviewReferenceResourcesUseCase(
    private val generationRepository: ComfyUIGenerationRepository,
    private val modelRepository: ModelRepository,
    private val schema: ExtractWorkflowParametersUseCase,
) {
    suspend operator fun invoke(
        prepared: PreparedReferenceGeneration,
        source: SourceReference,
        targetFileHashes: List<TargetFileHash> = emptyList(),
        ownedSha256: Set<String> = emptySet(),
    ): ReferenceResourceReview {
        val objectInfo = fetchObjectInfo(prepared.target)
        val capability = objectInfo.fold(
            onSuccess = { checkGraph(prepared.workflowJson, it) },
            onFailure = { TargetCapability.Unavailable(it.message ?: it::class.simpleName.orEmpty()) },
        )
        val targetFiles = objectInfo.getOrNull()?.let(::modelFileChoices)
        val evidence = HashEvidence(
            prepared.target,
            targetFileHashes,
            ownedSha256.mapTo(mutableSetOf()) { it.lowercase() },
        )
        val selectedFiles = selectedFiles(prepared.effectiveSettings)
        val resources = source.resources.map { reviewResource(it, targetFiles, selectedFiles, evidence) }
        return ReferenceResourceReview(
            target = prepared.target,
            capability = capability,
            resources = resources,
            settings = compareSettings(source, prepared.effectiveSettings, resources),
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchObjectInfo(target: ComfyUIJobTarget): Result<JsonObject> = try {
        val body = generationRepository.fetchObjectInfo(target)
        Result.success(Json.parseToJsonElement(body).jsonObject)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun checkGraph(workflowJson: String, objectInfo: JsonObject): TargetCapability {
        val graph = Json.parseToJsonElement(workflowJson).jsonObject
        val problems = graph.values.flatMap { nodeProblems(it.jsonObject, objectInfo) }.distinct()
        return if (problems.isEmpty()) TargetCapability.Supported else TargetCapability.Blocked(problems)
    }

    private fun nodeProblems(node: JsonObject, objectInfo: JsonObject): List<CapabilityProblem> {
        val classType = node["class_type"]?.jsonPrimitive?.content ?: return emptyList()
        val nodeSchema = objectInfo[classType] as? JsonObject
            ?: return listOf(CapabilityProblem.MissingNode(classType))
        val inputs = node["inputs"] as? JsonObject ?: return emptyList()
        // Links are arrays and numbers have no choices; only string inputs name files or options.
        return inputs.mapNotNull { (input, value) ->
            val text = (value as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return@mapNotNull null
            val info = schema.resolveSchemaInfo(input, nodeSchema)
                ?: return@mapNotNull CapabilityProblem.UnknownInput(classType, input)
            // A server with no files of a kind lists an empty choice, which still rules the file out.
            val restricted = info.options.isNotEmpty() || input in FILE_INPUTS
            CapabilityProblem.UnsupportedChoice(classType, input, text)
                .takeIf { restricted && text !in info.options }
        }
    }

    private fun modelFileChoices(objectInfo: JsonObject): List<String> = MODEL_FILE_INPUTS.flatMap { (node, input) ->
        schema.resolveSchemaInfo(input, objectInfo[node] as? JsonObject)?.options.orEmpty()
    }.distinct()

    private fun selectedFiles(settings: EffectiveGenerationSettings): Set<String> =
        setOf(effectiveModel(settings)) + settings.loraSelections.map { it.name }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    private suspend fun reviewResource(
        resource: SourceResource,
        targetFiles: List<String>?,
        selectedFiles: Set<String>,
        evidence: HashEvidence,
    ): ResourceReview {
        fun review(confidence: ResourceConfidence, reason: String) =
            ResourceReview(resource.versionId, resource.type, confidence, reason)

        val version = try {
            modelRepository.getModelVersion(resource.versionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return review(ResourceConfidence.UNKNOWN, "Version lookup failed: ${e.message}")
        }
        val names = version.files.map { baseName(it.name) }.filter { it.isNotBlank() }.toSet()
        if (names.isEmpty()) return review(ResourceConfidence.UNKNOWN, "The version lists no files")
        val versionHashes = version.files.mapNotNullTo(mutableSetOf()) { it.hashes[SHA256_KEY]?.lowercase() }
        val owned = versionHashes.any { it in evidence.ownedSha256 }
        if (targetFiles == null) {
            return review(
                ResourceConfidence.UNKNOWN,
                "The target's file list is unavailable"
            ).copy(ownedLocally = owned)
        }
        val matches = targetFiles.filter { baseName(it) in names }
        val (confidence, reason) = identify(matches, names, versionHashes, evidence)
        return review(confidence, reason).copy(
            targetFiles = matches,
            selected = matches.any { it in selectedFiles },
            ownedLocally = owned,
        )
    }

    private fun identify(
        matches: List<String>,
        names: Set<String>,
        versionHashes: Set<String>,
        evidence: HashEvidence,
    ): Pair<ResourceConfidence, String> {
        if (matches.isEmpty()) return ResourceConfidence.MISSING to "No target file is named ${names.joinToString()}"
        val known = evidence.targetFileHashes.filter { it.target == evidence.target && it.fileName in matches }
        val verified = known.firstOrNull { it.sha256.lowercase() in versionHashes }
        return when {
            verified != null -> ResourceConfidence.VERIFIED to "SHA-256 of ${verified.fileName} on this target matches"
            known.isNotEmpty() && versionHashes.isNotEmpty() ->
                ResourceConfidence.MISSING to "SHA-256 of ${known.joinToString { it.fileName }} differs"
            else -> ResourceConfidence.FILENAME_ONLY to "Only the file name matches: ${matches.joinToString()}"
        }
    }

    private fun compareSettings(
        source: SourceReference,
        effective: EffectiveGenerationSettings,
        resources: List<ResourceReview>,
    ): List<SettingComparison> {
        val gen = source.generation
        val model = effectiveModel(effective)
        // A selector fallback picks some checkpoint; only a file that matched a recorded version counts.
        val modelKind = when {
            resources.any { model in it.targetFiles } -> ComparisonKind.MATCH
            gen?.model == null && resources.none { it.type.equals(CHECKPOINT, ignoreCase = true) } ->
                ComparisonKind.SOURCE_UNAVAILABLE
            else -> ComparisonKind.VARIATION
        }
        return listOf(
            compare(ReferenceSetting.PROMPT, gen?.prompt, effective.prompt),
            SettingComparison(ReferenceSetting.MODEL, gen?.model, model, modelKind),
            compare(ReferenceSetting.DIMENSIONS, gen?.size?.normalizedSize(), "${effective.width}x${effective.height}"),
            compare(ReferenceSetting.STEPS, gen?.steps?.toString(), effective.steps.toString()),
            compare(ReferenceSetting.CFG, gen?.cfgScale?.toString(), effective.cfgScale.toString()),
            compare(ReferenceSetting.SAMPLER, gen?.sampler?.normalizedSampler(), effective.samplerName),
            // The source snapshot records no scheduler.
            compare(ReferenceSetting.SCHEDULER, null, effective.scheduler),
            compare(ReferenceSetting.SEED, gen?.seed?.toString(), effective.seed.toString()),
        ) + compareLoraWeights(source.resources, effective, resources)
    }

    private fun compareLoraWeights(
        sourceResources: List<SourceResource>,
        effective: EffectiveGenerationSettings,
        resources: List<ResourceReview>,
    ): List<SettingComparison> {
        val used = effective.loraSelections.map { lora ->
            val match = resources.firstOrNull { lora.name in it.targetFiles }
            val weight = sourceResources.firstOrNull { it.versionId == match?.versionId }?.weight
            when {
                match == null -> SettingComparison(
                    ReferenceSetting.LORA_WEIGHT,
                    null,
                    lora.strengthModel.toString(),
                    ComparisonKind.VARIATION,
                    lora.name,
                )
                // Floats print their shortest form, so compare at the selection's precision.
                else -> compare(
                    ReferenceSetting.LORA_WEIGHT,
                    weight?.toFloat()?.toString(),
                    lora.strengthModel.toString(),
                    lora.name,
                )
            }
        }
        val dropped = sourceResources.filter { it.type.equals(LORA, ignoreCase = true) }
            .filter { res -> resources.none { it.versionId == res.versionId && it.selected } }
            .map {
                SettingComparison(
                    ReferenceSetting.LORA_WEIGHT,
                    it.weight?.toString(),
                    null,
                    ComparisonKind.VARIATION,
                    "version ${it.versionId}",
                )
            }
        return used + dropped
    }

    private fun compare(setting: ReferenceSetting, source: String?, effective: String, subject: String? = null) =
        SettingComparison(
            setting = setting,
            source = source,
            effective = effective,
            kind = when (source) {
                null -> ComparisonKind.SOURCE_UNAVAILABLE
                effective -> ComparisonKind.MATCH
                else -> ComparisonKind.VARIATION
            },
            subject = subject,
        )

    private class HashEvidence(
        val target: ComfyUIJobTarget,
        val targetFileHashes: List<TargetFileHash>,
        val ownedSha256: Set<String>,
    )

    private companion object {
        const val SHA256_KEY = "SHA256"
        const val CHECKPOINT = "checkpoint"
        const val LORA = "lora"
        val FILE_INPUTS = setOf("ckpt_name", "unet_name", "lora_name", "clip_name", "vae_name")
        val MODEL_FILE_INPUTS = listOf(
            "CheckpointLoaderSimple" to "ckpt_name",
            "UNETLoader" to "unet_name",
            "LoraLoader" to "lora_name",
        )

        fun effectiveModel(settings: EffectiveGenerationSettings): String =
            settings.diffusionModel?.unetName ?: settings.checkpoint

        fun baseName(path: String): String = path.substringAfterLast('/').substringAfterLast('\\')

        fun String.normalizedSize(): String = lowercase().replace(" ", "")

        // Mirrors the prefill's mapping, so an A1111 name that maps to the effective one matches.
        fun String.normalizedSampler(): String = lowercase().replace(" ", "_").replace("-", "_")
    }
}
