package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.feature.comfyui.domain.model.ExtractedParameter
import com.riox432.civitdeck.feature.comfyui.domain.model.ParameterType
import com.riox432.civitdeck.util.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Injects modified parameter values back into a ComfyUI workflow JSON before submission.
 */
class InjectWorkflowParametersUseCase {

    /**
     * @param workflowJson The original workflow JSON string.
     * @param parameters List of parameters with updated [ExtractedParameter.currentValue].
     * @return The modified workflow JSON string with injected values.
     */
    operator fun invoke(
        workflowJson: String,
        parameters: List<ExtractedParameter>,
    ): String {
        val workflow = try {
            Json.parseToJsonElement(workflowJson).jsonObject
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Failed to parse workflow JSON: ${e.message}")
            return workflowJson
        }

        // Group parameters by node ID for efficient lookup
        val paramsByNode = parameters.groupBy { it.nodeId }

        val modifiedWorkflow = buildJsonObject {
            for ((nodeId, nodeElement) in workflow) {
                val node = nodeElement as? JsonObject
                val nodeParams = paramsByNode[nodeId]

                if (node == null || nodeParams.isNullOrEmpty()) {
                    put(nodeId, nodeElement)
                    continue
                }

                put(nodeId, injectNodeParams(node, nodeParams))
            }
        }

        return Json.encodeToString(JsonObject.serializer(), modifiedWorkflow)
    }

    private fun injectNodeParams(
        node: JsonObject,
        params: List<ExtractedParameter>,
    ): JsonObject {
        val inputs = node["inputs"] as? JsonObject ?: return node
        val paramMap = params.associateBy { it.paramName }

        val modifiedInputs = buildJsonObject {
            for ((key, value) in inputs) {
                val param = paramMap[key]
                if (param != null) {
                    put(key, toJsonPrimitive(param))
                } else {
                    put(key, value)
                }
            }
        }

        return buildJsonObject {
            for ((key, value) in node) {
                if (key == "inputs") {
                    put("inputs", modifiedInputs)
                } else {
                    put(key, value)
                }
            }
        }
    }

    private fun toJsonPrimitive(param: ExtractedParameter): JsonPrimitive {
        val value = param.currentValue
        return when (param.paramType) {
            ParameterType.SEED -> {
                val longVal = value.toLongOrNull() ?: -1L
                JsonPrimitive(longVal)
            }
            ParameterType.NUMBER -> {
                // Try long first (for integer fields like steps, width, height)
                val longVal = value.toLongOrNull()
                if (longVal != null) {
                    JsonPrimitive(longVal)
                } else {
                    val doubleVal = value.toDoubleOrNull() ?: 0.0
                    JsonPrimitive(doubleVal)
                }
            }
            ParameterType.TEXT, ParameterType.SELECT -> JsonPrimitive(value)
        }
    }

    companion object {
        private const val TAG = "InjectWorkflowParams"
    }
}
