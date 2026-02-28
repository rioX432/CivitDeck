package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.api.comfyui.HistoryEntry
import com.riox432.civitdeck.data.local.dao.ComfyUIConnectionDao
import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import com.riox432.civitdeck.domain.model.ComfyUIGenerationMeta
import com.riox432.civitdeck.domain.repository.ComfyUIHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long

class ComfyUIHistoryRepositoryImpl(
    private val dao: ComfyUIConnectionDao,
    private val api: ComfyUIApi,
) : ComfyUIHistoryRepository {

    override fun fetchHistory(): Flow<List<ComfyUIGeneratedImage>> = flow {
        ensureApiConfigured()
        val historyMap = api.getAllHistory()
        println("DEBUG ComfyUI: fetched ${historyMap.size} history entries")
        val images = historyMap.flatMap { (promptId, entry) ->
            entry.toGeneratedImages(promptId)
        }
        println("DEBUG ComfyUI: total images=${images.size}, first imageUrl=${images.firstOrNull()?.imageUrl}")
        emit(images)
    }

    override fun fetchHistoryItem(promptId: String): Flow<List<ComfyUIGeneratedImage>> = flow {
        ensureApiConfigured()
        val entry = api.getHistory(promptId)
        val images = entry?.toGeneratedImages(promptId) ?: emptyList()
        emit(images)
    }

    private fun HistoryEntry.toGeneratedImages(promptId: String): List<ComfyUIGeneratedImage> {
        val meta = extractMeta(this)
        return outputs.values.flatMap { nodeOutput ->
            (nodeOutput.images ?: emptyList()).map { imgRef ->
                val imageUrl = api.getImageUrl(imgRef)
                ComfyUIGeneratedImage(
                    id = "$promptId/${imgRef.filename}",
                    promptId = promptId,
                    filename = imgRef.filename,
                    subfolder = imgRef.subfolder,
                    type = imgRef.type,
                    imageUrl = imageUrl,
                    meta = meta,
                )
            }
        }
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

    private suspend fun ensureApiConfigured() {
        val active = dao.getActive() ?: error("No active ComfyUI connection")
        api.setBaseUrl(active.hostname, active.port)
    }
}
