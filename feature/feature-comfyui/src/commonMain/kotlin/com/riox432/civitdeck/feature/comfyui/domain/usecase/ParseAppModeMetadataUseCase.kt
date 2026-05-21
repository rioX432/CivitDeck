package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.AppModeInput
import com.riox432.civitdeck.domain.model.AppModeMetadata
import com.riox432.civitdeck.domain.model.AppModeOutput
import com.riox432.civitdeck.domain.model.AppModeView
import com.riox432.civitdeck.util.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses ComfyUI APP mode metadata from a workflow JSON.
 *
 * APP mode metadata is stored in the workflow's `extra` field:
 * - `extra.linearData.inputs`: Array of [nodeId, paramName, config?] tuples
 * - `extra.linearData.outputs`: Array of nodeId strings
 * - `extra.linearMode`: Boolean (true = app view, false = graph view)
 *
 * Returns null if the workflow does not contain APP mode metadata.
 */
class ParseAppModeMetadataUseCase {

    /**
     * @param workflowJson The full workflow JSON string (not API format).
     * @return Parsed APP mode metadata, or null if not present.
     */
    operator fun invoke(workflowJson: String): AppModeMetadata? {
        val root = try {
            Json.parseToJsonElement(workflowJson).jsonObject
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Failed to parse workflow JSON: ${e.message}")
            return null
        }

        val extra = root["extra"] as? JsonObject ?: return null
        val linearData = extra["linearData"] as? JsonObject ?: return null

        val inputs = parseInputs(linearData, root)
        val outputs = parseOutputs(linearData, root)
        val defaultView = parseDefaultView(extra)

        // If there are no inputs and no outputs, treat as no APP mode
        if (inputs.isEmpty() && outputs.isEmpty()) return null

        return AppModeMetadata(
            inputs = inputs,
            outputs = outputs,
            defaultView = defaultView,
        )
    }

    private fun parseInputs(
        linearData: JsonObject,
        root: JsonObject,
    ): List<AppModeInput> {
        val inputsArray = linearData["inputs"] as? JsonArray ?: return emptyList()
        return inputsArray.mapIndexedNotNull { index, element ->
            parseSingleInput(element, index, root)
        }
    }

    /**
     * Parse a single input entry.
     * Format: [nodeId, paramName] or [nodeId, paramName, {height?: number}]
     */
    private fun parseSingleInput(
        element: kotlinx.serialization.json.JsonElement,
        index: Int,
        root: JsonObject,
    ): AppModeInput? {
        val tuple = element as? JsonArray ?: return null
        if (tuple.size < INPUT_TUPLE_MIN_SIZE) return null

        val nodeId = (tuple[0] as? JsonPrimitive)?.content ?: return null
        val paramName = (tuple[1] as? JsonPrimitive)?.content ?: return null

        val config = if (tuple.size > INPUT_TUPLE_MIN_SIZE) {
            tuple[INPUT_TUPLE_MIN_SIZE] as? JsonObject
        } else {
            null
        }
        val widgetHeight = config?.get("height")?.jsonPrimitive?.intOrNull

        // Derive label from node's _meta.title if available
        val label = resolveNodeTitle(root, nodeId)

        return AppModeInput(
            nodeId = nodeId,
            paramName = paramName,
            label = label,
            group = null,
            order = index,
            widgetHeight = widgetHeight,
        )
    }

    private fun parseOutputs(
        linearData: JsonObject,
        root: JsonObject,
    ): List<AppModeOutput> {
        val outputsArray = linearData["outputs"] as? JsonArray ?: return emptyList()
        return outputsArray.mapNotNull { element ->
            val nodeId = (element as? JsonPrimitive)?.content ?: return@mapNotNull null
            val label = resolveNodeTitle(root, nodeId)
            AppModeOutput(nodeId = nodeId, label = label)
        }
    }

    private fun parseDefaultView(extra: JsonObject): AppModeView {
        val linearMode = extra["linearMode"]?.jsonPrimitive?.booleanOrNull
        return if (linearMode == true) AppModeView.APP else AppModeView.GRAPH
    }

    /**
     * Looks up the node's _meta.title from the workflow JSON.
     * Workflow nodes are stored as top-level entries (nodeId -> nodeObject)
     * or inside a "nodes" array with "id" fields.
     */
    private fun resolveNodeTitle(root: JsonObject, nodeId: String): String? {
        // API format: top-level nodeId keys
        val nodeObj = root[nodeId] as? JsonObject
        if (nodeObj != null) {
            return extractMetaTitle(nodeObj)
        }

        // UI workflow format: "nodes" array with "id" field
        val nodesArray = root["nodes"] as? JsonArray ?: return null
        val node = nodesArray.firstOrNull { entry ->
            val obj = entry as? JsonObject ?: return@firstOrNull false
            val id = (obj["id"] as? JsonPrimitive)?.content
            id == nodeId
        } as? JsonObject
        return node?.let { extractMetaTitle(it) }
            ?: node?.let { (it["title"] as? JsonPrimitive)?.content }
    }

    private fun extractMetaTitle(node: JsonObject): String? {
        val meta = node["_meta"] as? JsonObject ?: return null
        return (meta["title"] as? JsonPrimitive)?.content
    }

    companion object {
        private const val TAG = "ParseAppModeMetadata"
        private const val INPUT_TUPLE_MIN_SIZE = 2
    }
}
