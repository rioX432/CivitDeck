package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.AppModeInput
import com.riox432.civitdeck.domain.model.AppModeMetadata
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
 * Parses a ComfyUI workflow JSON and extracts editable parameters.
 *
 * Extraction strategy:
 * 1. Try APP mode: parse `extra.linearData` for designated inputs (official ComfyUI standard).
 * 2. Fallback: extract from hardcoded PRIORITY_NODES when no APP mode metadata is present.
 *
 * Both paths are optionally enriched with /object_info schema data.
 */
class ExtractWorkflowParametersUseCase(
    private val parseAppModeMetadata: ParseAppModeMetadataUseCase,
) {

    /**
     * @param workflowJson The raw workflow JSON string.
     * @param objectInfoJson Optional /object_info response JSON for enriching param metadata.
     * @return List of extracted parameters sorted by APP mode order or node priority.
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

        val objectInfo = parseObjectInfo(objectInfoJson)
        val appMode = parseAppModeMetadata(workflowJson)

        return if (appMode != null) {
            Logger.d(TAG, "APP mode detected: ${appMode.inputs.size} inputs")
            extractAppModeParameters(appMode, workflow, objectInfo)
        } else {
            extractLegacyParameters(workflow, objectInfo)
        }
    }

    /**
     * Extract parameters designated by APP mode metadata.
     * Only inputs explicitly listed in linearData.inputs are extracted.
     */
    private fun extractAppModeParameters(
        appMode: AppModeMetadata,
        workflow: JsonObject,
        objectInfo: JsonObject?,
    ): List<ExtractedParameter> {
        return appMode.inputs.mapNotNull { input ->
            extractAppModeInput(input, workflow, objectInfo)
        }
    }

    private fun extractAppModeInput(
        input: AppModeInput,
        workflow: JsonObject,
        objectInfo: JsonObject?,
    ): ExtractedParameter? {
        val node = workflow[input.nodeId] as? JsonObject ?: return null
        val classType = (node["class_type"] as? JsonPrimitive)?.content ?: return null
        val inputs = node["inputs"] as? JsonObject ?: return null

        val rawValue = inputs[input.paramName] ?: return null
        // Skip node link references (arrays like ["3", 0])
        if (rawValue is JsonArray) return null

        val currentValue = when (rawValue) {
            is JsonPrimitive -> rawValue.content
            else -> rawValue.toString()
        }

        val title = input.label ?: extractNodeTitle(node, classType, input.nodeId)
        val nodeSchema = objectInfo?.get(classType) as? JsonObject
        val schemaInfo = resolveSchemaInfo(input.paramName, nodeSchema)
        val paramType = resolveParameterType(input.paramName, schemaInfo)

        return ExtractedParameter(
            nodeId = input.nodeId,
            nodeTitle = title,
            nodeClassType = classType,
            paramName = input.paramName,
            paramType = paramType,
            currentValue = currentValue,
            min = schemaInfo?.min,
            max = schemaInfo?.max,
            step = schemaInfo?.step,
            options = schemaInfo?.options ?: emptyList(),
            group = input.group,
            order = input.order,
        )
    }

    // region Legacy extraction (PRIORITY_NODES fallback)

    private fun extractLegacyParameters(
        workflow: JsonObject,
        objectInfo: JsonObject?,
    ): List<ExtractedParameter> {
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

    // endregion

    // region Shared helpers

    private fun parseObjectInfo(objectInfoJson: String?): JsonObject? {
        return objectInfoJson?.let {
            try {
                Json.parseToJsonElement(it).jsonObject
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w(TAG, "Failed to parse object_info JSON: ${e.message}")
                null
            }
        }
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

        // Image upload fields
        if (paramName == "image" || paramName in KNOWN_IMAGE_PARAMS) return ParameterType.IMAGE

        // Boolean fields (schema type or known names)
        if (schema?.isBoolean == true || paramName in KNOWN_BOOLEAN_PARAMS) {
            return ParameterType.BOOLEAN
        }

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

    internal data class SchemaInfo(
        val min: Double? = null,
        val max: Double? = null,
        val step: Double? = null,
        val options: List<String> = emptyList(),
        val isBoolean: Boolean = false,
    )

    /**
     * Resolves parameter schema from /object_info response.
     * Object info structure: { NodeType: { input: { required: { paramName: [...] } } } }
     */
    @Suppress("ReturnCount")
    internal fun resolveSchemaInfo(paramName: String, nodeSchema: JsonObject?): SchemaInfo? {
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
            val typeStr = first.content
            val isBoolean = typeStr.equals("BOOLEAN", ignoreCase = true)
            val constraints = if (paramDef.size > 1) paramDef[1] as? JsonObject else null
            return SchemaInfo(
                min = constraints?.get("min")?.jsonPrimitive?.doubleOrNull,
                max = constraints?.get("max")?.jsonPrimitive?.doubleOrNull,
                step = constraints?.get("step")?.jsonPrimitive?.doubleOrNull,
                isBoolean = isBoolean,
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

    // endregion

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

        private val KNOWN_IMAGE_PARAMS = setOf(
            "image", "input_image", "source_image", "mask",
        )

        private val KNOWN_BOOLEAN_PARAMS = setOf(
            "add_noise", "return_with_leftover_noise", "tiling",
            "disable_noise", "force_inpaint",
        )
    }
}
