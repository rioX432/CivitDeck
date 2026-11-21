package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.api.comfyui.HistoryEntry
import com.riox432.civitdeck.data.api.comfyui.HistoryStatus
import com.riox432.civitdeck.domain.model.ComfyUIBoundHistoryItem
import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import com.riox432.civitdeck.domain.model.ComfyUIGenerationMeta
import com.riox432.civitdeck.domain.model.ComfyUIHistoryOutcome
import com.riox432.civitdeck.domain.model.ComfyUIHistoryPage
import com.riox432.civitdeck.domain.model.ComfyUIJobHandle
import com.riox432.civitdeck.domain.model.ComfyUIJobTarget
import com.riox432.civitdeck.domain.repository.ComfyUIHistoryRepository
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long

class ComfyUIHistoryRepositoryImpl(
    private val apiProvider: ComfyUIApiProvider,
) : ComfyUIHistoryRepository {

    // The active target is captured inside each collection, before the request, so every output
    // of a page carries the server that answered it even if another is activated meanwhile.
    override fun fetchHistory(maxItems: Int): Flow<ComfyUIHistoryPage> = flow {
        val target = apiProvider.captureActiveTarget()
        val api = apiProvider.forTarget(target).api
        val historyMap = api.getRecentHistory(maxItems)
        val images = historyMap.flatMap { (promptId, entry) ->
            entry.toGeneratedImages(api, promptId, target)
        }
        emit(ComfyUIHistoryPage(images = images, hasMore = historyMap.size >= maxItems))
    }

    override fun fetchHistoryItem(promptId: String): Flow<List<ComfyUIGeneratedImage>> = flow {
        val target = apiProvider.captureActiveTarget()
        val api = apiProvider.forTarget(target).api
        val entry = api.getHistory(promptId)
        emit(entry?.toGeneratedImages(api, promptId, target) ?: emptyList())
    }

    override fun fetchHistoryItem(job: ComfyUIJobHandle): Flow<ComfyUIBoundHistoryItem> = flow {
        val api = apiProvider.forTarget(job.target).api
        val entry = api.getHistory(job.promptId)
        if (entry == null) {
            emit(
                ComfyUIBoundHistoryItem(
                    job = job,
                    present = false,
                    outcome = ComfyUIHistoryOutcome.Unknown,
                    images = emptyList(),
                ),
            )
            return@flow
        }
        val outcome = entry.outcome(job.promptId)
        emit(
            ComfyUIBoundHistoryItem(
                job = job,
                present = true,
                outcome = outcome,
                images = entry.toGeneratedImages(api, job.promptId, job.target),
                error = if (outcome == ComfyUIHistoryOutcome.Failed) entry.status?.executionErrorMessage else null,
            ),
        )
    }

    // The /view URLs come from the same endpoint as the entry, so they point at [target].
    private fun HistoryEntry.toGeneratedImages(
        api: ComfyUIApi,
        promptId: String,
        target: ComfyUIJobTarget,
    ): List<ComfyUIGeneratedImage> {
        val meta = extractMeta(this)
        return outputs.values.flatMap { nodeOutput ->
            (nodeOutput.images ?: emptyList()).map { imgRef ->
                val imageUrl = "${api.getImageUrl(imgRef)}&_prompt=$promptId"
                ComfyUIGeneratedImage(
                    id = "$promptId/${imgRef.filename}",
                    promptId = promptId,
                    filename = imgRef.filename,
                    subfolder = imgRef.subfolder,
                    type = imgRef.type,
                    imageUrl = imageUrl,
                    meta = meta,
                    jobTarget = target,
                )
            }
        }
    }

    /**
     * ComfyUI records `status_str: "error"` with `completed: false` for both a failed and an
     * interrupted prompt, so only the `execution_interrupted` event tells them apart. As in
     * generation polling, `status_str: "success"` or `completed: true` without an error is success;
     * anything else is not a confirmed outcome.
     */
    private fun HistoryEntry.outcome(promptId: String): ComfyUIHistoryOutcome {
        val status = status ?: return ComfyUIHistoryOutcome.Unknown
        val events = status.eventsFor(promptId)
        return when {
            EVENT_INTERRUPTED in events -> ComfyUIHistoryOutcome.Interrupted
            EVENT_ERROR in events || status.statusStr == STATUS_ERROR -> ComfyUIHistoryOutcome.Failed
            status.statusStr == STATUS_SUCCESS || status.completed == true -> ComfyUIHistoryOutcome.Completed
            else -> ComfyUIHistoryOutcome.Unknown
        }
    }

    /** Names of the `[event, data]` messages whose data names [promptId]; malformed ones are skipped. */
    private fun HistoryStatus.eventsFor(promptId: String): List<String> =
        (messages as? JsonArray).orEmpty().mapNotNull { message ->
            val pair = message as? JsonArray ?: return@mapNotNull null
            val event = (pair.getOrNull(0) as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val data = pair.getOrNull(1) as? JsonObject ?: return@mapNotNull null
            val dataPromptId = (data["prompt_id"] as? JsonPrimitive)?.contentOrNull
            event.takeIf { dataPromptId == promptId }
        }

    /**
     * Extracts generation metadata from the raw prompt array stored in HistoryEntry.
     * ComfyUI history stores the prompt as: [index, prompt_id, {nodes}, extra_data, outputs_to_execute]
     * We scan node inputs for KSampler (seed, cfg, steps, sampler_name),
     * CLIPTextEncode (positive prompt text), and LoraLoader (lora_name).
     */
    private fun extractMeta(entry: HistoryEntry): ComfyUIGenerationMeta {
        val promptNodes = entry.promptNodes ?: return ComfyUIGenerationMeta()
        val validNodes = promptNodes.values.mapNotNull { it as? JsonObject }
        val samplerMeta = validNodes.firstOrNull { node ->
            (node["class_type"] as? JsonPrimitive)?.content == "KSampler"
        }?.let { extractSamplerMeta(it) }
        val positivePrompt = validNodes.firstNotNullOfOrNull { node ->
            extractClipText(node)
        } ?: ""
        val loraNames = validNodes.mapNotNull { node ->
            extractLoraName(node)
        }
        return ComfyUIGenerationMeta(
            positivePrompt = positivePrompt,
            seed = samplerMeta?.seed,
            samplerName = samplerMeta?.samplerName,
            cfg = samplerMeta?.cfg,
            steps = samplerMeta?.steps,
            loraNames = loraNames,
        )
    }

    private data class SamplerMeta(
        val seed: Long?,
        val samplerName: String?,
        val cfg: Double?,
        val steps: Int?,
    )

    private fun extractSamplerMeta(node: JsonObject): SamplerMeta {
        val inputs = node["inputs"] as? JsonObject ?: return SamplerMeta(null, null, null, null)
        return SamplerMeta(
            seed = (inputs["seed"] as? JsonPrimitive)?.runCatching { long }?.getOrNull(),
            samplerName = (inputs["sampler_name"] as? JsonPrimitive)?.content,
            cfg = (inputs["cfg"] as? JsonPrimitive)?.runCatching { double }?.getOrNull(),
            steps = (inputs["steps"] as? JsonPrimitive)?.runCatching { int }?.getOrNull(),
        )
    }

    private fun extractClipText(node: JsonObject): String? {
        if ((node["class_type"] as? JsonPrimitive)?.content != "CLIPTextEncode") return null
        val inputs = node["inputs"] as? JsonObject ?: return null
        val text = (inputs["text"] as? JsonPrimitive)?.content ?: return null
        return text.takeIf { it.isNotBlank() }
    }

    private fun extractLoraName(node: JsonObject): String? {
        if ((node["class_type"] as? JsonPrimitive)?.content != "LoraLoader") return null
        val inputs = node["inputs"] as? JsonObject ?: return null
        return (inputs["lora_name"] as? JsonPrimitive)?.content
    }

    private companion object {
        const val STATUS_SUCCESS = "success"
        const val STATUS_ERROR = "error"
        const val EVENT_ERROR = "execution_error"
        const val EVENT_INTERRUPTED = "execution_interrupted"
    }
}
