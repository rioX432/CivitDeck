package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.feature.comfyui.domain.model.ExtractedParameter
import com.riox432.civitdeck.feature.comfyui.domain.model.ParameterType
import com.riox432.civitdeck.util.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses a ComfyUI workflow JSON and extracts editable parameters from priority node types.
 * Optionally uses /object_info data to enrich parameter metadata (options, ranges).
 */
class ExtractWorkflowParametersUseCase {

    /**
     * @param workflowJson The raw workflow JSON string.
     * @param objectInfoJson Optional /object_info response JSON for enriching param metadata.
     * @return List of extracted parameters sorted by node priority then param order.
     */
    operator fun invoke(
        workflowJson: String,
        objectInfoJson: String? = null,
    ): List<ExtractedParameter> {
        val workflow = try {
            Json.parseToJsonElement(workflowJson).jsonObject
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Failed to parse workflow JSON: ${e.message}")
            return emptyList()
        }

        val objectInfo = objectInfoJson?.let {
            try {
                Json.parseToJsonElement(it).jsonObject
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w(TAG, "Failed to parse object_info JSON: ${e.message}")
                null
            }
        }

        return workflow.entries
            .mapNotNull { (nodeId, nodeElement) ->
                val node = nodeElement as? JsonObject ?: return@mapNotNull null
                extractNodeParameters(nodeId, node, objectInfo)
            }
            .flatten()
            .sortedWith(compareBy({ priorityOrder(it.nodeClassType) }, { it.nodeId }))
    }

    private fun extractNodeParameters(
        nodeId: String,
        node: JsonObject,
        objectInfo: JsonObject?,
    ): List<ExtractedParameter> {
        val classType = (node["class_type"] as? JsonPrimitive)?.content ?: return emptyList()

        // Only extract from priority node types
        val paramConfig = PRIORITY_NODES[classType] ?: return emptyList()

        val inputs = node["inputs"] as? JsonObject ?: return emptyList()
        val title = extractNodeTitle(node, classType, nodeId)
        val nodeSchema = objectInfo?.get(classType) as? JsonObject

        return paramConfig.mapNotNull { paramName ->
            extractSingleParam(nodeId, title, classType, paramName, inputs, nodeSchema)
        }
    }

    @Suppress("LongParameterList")
    private fun extractSingleParam(
        nodeId: String,
        title: String,
        classType: String,
        paramName: String,
        inputs: JsonObject,
        nodeSchema: JsonObject?,
    ): ExtractedParameter? {
        val rawValue = inputs[paramName] ?: return null

        // Skip node link references (arrays like ["3", 0])
        if (rawValue is JsonArray) return null

        val currentValue = when (rawValue) {
            is JsonPrimitive -> rawValue.content
            else -> rawValue.toString()
        }

        val schemaInfo = resolveSchemaInfo(paramName, nodeSchema)
        val paramType = resolveParameterType(paramName, schemaInfo)

        return ExtractedParameter(
            nodeId = nodeId,
            nodeTitle = title,
            nodeClassType = classType,
            paramName = paramName,
            paramType = paramType,
            currentValue = currentValue,
            min = schemaInfo?.min,
            max = schemaInfo?.max,
            step = schemaInfo?.step,
            options = schemaInfo?.options ?: emptyList(),
        )
    }

    private fun extractNodeTitle(node: JsonObject, classType: String, nodeId: String): String {
        // ComfyUI workflows may have _meta.title
        val meta = node["_meta"] as? JsonObject
        val metaTitle = (meta?.get("title") as? JsonPrimitive)?.content
        return metaTitle ?: "$classType #$nodeId"
    }

    @Suppress("ReturnCount")
    private fun resolveParameterType(
        paramName: String,
        schema: SchemaInfo?,
    ): ParameterType {
        // Seed fields are always SEED type
        if (paramName == "seed" || paramName == "noise_seed") return ParameterType.SEED

        // If schema has options, it's a SELECT
        if (schema != null && schema.options.isNotEmpty()) return ParameterType.SELECT

        // Known text fields
        if (paramName == "text" || paramName == "filename_prefix") return ParameterType.TEXT

        // Known numeric fields or if schema has min/max
        if (schema?.min != null || schema?.max != null) return ParameterType.NUMBER
        if (paramName in KNOWN_NUMERIC_PARAMS) return ParameterType.NUMBER

        // Fallback: default to text input
        return ParameterType.TEXT
    }

    private data class SchemaInfo(
        val min: Double? = null,
        val max: Double? = null,
        val step: Double? = null,
        val options: List<String> = emptyList(),
    )

    /**
     * Resolves parameter schema from /object_info response.
     * Object info structure: { NodeType: { input: { required: { paramName: [...] } } } }
     */
    @Suppress("ReturnCount")
    private fun resolveSchemaInfo(paramName: String, nodeSchema: JsonObject?): SchemaInfo? {
        if (nodeSchema == null) return null

        val inputObj = nodeSchema["input"] as? JsonObject ?: return null
        val requiredObj = inputObj["required"] as? JsonObject
        val optionalObj = inputObj["optional"] as? JsonObject

        val paramDef = requiredObj?.get(paramName) as? JsonArray
            ?: optionalObj?.get(paramName) as? JsonArray
            ?: return null

        return parseParamDefinition(paramDef)
    }

    @Suppress("ReturnCount")
    private fun parseParamDefinition(paramDef: JsonArray): SchemaInfo {
        if (paramDef.isEmpty()) return SchemaInfo()

        val first = paramDef[0]

        // If first element is an array, it's a list of options
        if (first is JsonArray) {
            val options = first.mapNotNull { (it as? JsonPrimitive)?.content }
            return SchemaInfo(options = options)
        }

        // If first element is a string type descriptor, check for constraints in second element
        if (first is JsonPrimitive) {
            val constraints = if (paramDef.size > 1) paramDef[1] as? JsonObject else null
            return SchemaInfo(
                min = constraints?.get("min")?.jsonPrimitive?.doubleOrNull,
                max = constraints?.get("max")?.jsonPrimitive?.doubleOrNull,
                step = constraints?.get("step")?.jsonPrimitive?.doubleOrNull,
            )
        }

        return SchemaInfo()
    }

    private fun priorityOrder(classType: String): Int = when (classType) {
        "KSampler", "KSamplerAdvanced" -> 0
        "CLIPTextEncode" -> 1
        "CheckpointLoaderSimple" -> 2
        "LoraLoader" -> 3
        "EmptyLatentImage" -> 4
        else -> 5
    }

    companion object {
        private const val TAG = "ExtractWorkflowParams"

        /** Priority node types and the parameters to extract from each. */
        val PRIORITY_NODES: Map<String, List<String>> = mapOf(
            "KSampler" to listOf(
                "seed", "steps", "cfg", "sampler_name", "scheduler", "denoise",
            ),
            "KSamplerAdvanced" to listOf(
                "noise_seed", "steps", "cfg", "sampler_name", "scheduler",
                "start_at_step", "end_at_step",
            ),
            "CLIPTextEncode" to listOf("text"),
            "CheckpointLoaderSimple" to listOf("ckpt_name"),
            "LoraLoader" to listOf(
                "lora_name", "strength_model", "strength_clip",
            ),
            "EmptyLatentImage" to listOf("width", "height", "batch_size"),
        )

        private val KNOWN_NUMERIC_PARAMS = setOf(
            "steps", "cfg", "denoise", "strength_model", "strength_clip",
            "width", "height", "batch_size", "start_at_step", "end_at_step",
        )
    }
}
