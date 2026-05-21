package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.TemplateVariable
import com.riox432.civitdeck.domain.model.TemplateVariableType
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.model.WorkflowTemplateCategory
import com.riox432.civitdeck.domain.model.WorkflowTemplateType
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import com.riox432.civitdeck.domain.util.currentTimeMillis
import com.riox432.civitdeck.feature.comfyui.domain.model.ExtractedParameter
import com.riox432.civitdeck.feature.comfyui.domain.model.ParameterType
import com.riox432.civitdeck.util.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Imports a workflow template from JSON.
 *
 * Supports three import paths:
 * 1. Raw ComfyUI workflow with APP mode metadata -> auto-creates template from designated inputs
 * 2. Raw ComfyUI workflow without APP mode -> extracts parameters from PRIORITY_NODES
 * 3. Legacy ImportDto format -> manual template with explicit variable definitions
 */
class ImportWorkflowTemplateUseCase(
    private val repository: SavedPromptRepository,
    private val parseAppModeMetadata: ParseAppModeMetadataUseCase,
    private val extractWorkflowParameters: ExtractWorkflowParametersUseCase,
) {
    @Serializable
    private data class ImportDto(
        val name: String,
        val description: String = "",
        val type: String,
        val category: String = "GENERAL",
        val version: Int = 1,
        val author: String = "",
        val variables: List<TemplateVariableDto>,
    )

    suspend operator fun invoke(jsonString: String) {
        val trimmed = jsonString.trim()
        if (trimmed.isBlank()) error("Template JSON is empty")

        val jsonObj = tryParseJsonObject(trimmed)

        // Path 1: Try APP mode detection
        if (jsonObj != null) {
            val appMode = parseAppModeMetadata(trimmed)
            if (appMode != null) {
                Logger.d(TAG, "APP mode detected, creating template from metadata")
                val params = extractWorkflowParameters(trimmed)
                if (params.isNotEmpty()) {
                    val template = createTemplateFromExtracted(
                        jsonObj = jsonObj,
                        params = params,
                        rawJson = trimmed,
                        isAppMode = true,
                    )
                    repository.saveTemplate(template.toSavedPrompt())
                    return
                }
            }

            // Path 2: Try raw workflow extraction (PRIORITY_NODES)
            if (isRawWorkflow(jsonObj)) {
                val params = extractWorkflowParameters(trimmed)
                if (params.isNotEmpty()) {
                    Logger.d(TAG, "Raw workflow detected, extracting parameters")
                    val template = createTemplateFromExtracted(
                        jsonObj = jsonObj,
                        params = params,
                        rawJson = trimmed,
                        isAppMode = false,
                    )
                    repository.saveTemplate(template.toSavedPrompt())
                    return
                }
            }
        }

        // Path 3: Fall back to ImportDto format
        parseImportDto(trimmed)
    }

    private fun tryParseJsonObject(json: String): JsonObject? {
        return try {
            Json.parseToJsonElement(json).jsonObject
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            null
        }
    }

    /**
     * Checks if the JSON object looks like a raw ComfyUI workflow
     * (has node entries with class_type fields).
     */
    private fun isRawWorkflow(obj: JsonObject): Boolean {
        return obj.any { (_, value) ->
            (value as? JsonObject)?.containsKey("class_type") == true
        }
    }

    @Suppress("LongParameterList")
    private fun createTemplateFromExtracted(
        jsonObj: JsonObject,
        params: List<ExtractedParameter>,
        rawJson: String,
        isAppMode: Boolean,
    ): WorkflowTemplate {
        val name = resolveWorkflowName(jsonObj) ?: "Imported Workflow"
        val variables = params.map { it.toTemplateVariable() }
        val type = inferWorkflowType(params)

        return WorkflowTemplate(
            id = 0L,
            name = name,
            description = if (isAppMode) "Auto-detected from APP mode" else "Auto-extracted from workflow",
            type = type,
            category = WorkflowTemplateCategory.GENERAL,
            variables = variables,
            isBuiltIn = false,
            version = 1,
            createdAt = currentTimeMillis(),
            isAppMode = isAppMode,
            rawWorkflowJson = rawJson,
        )
    }

    /**
     * Tries to resolve a human-readable name from the workflow JSON.
     * Looks for common metadata fields like extra.title or a _meta.title on the first node.
     */
    private fun resolveWorkflowName(obj: JsonObject): String? {
        // Check extra.title
        val extra = obj["extra"] as? JsonObject
        val extraTitle = extra?.get("title")?.toString()?.trim('"')
        if (!extraTitle.isNullOrBlank()) return extraTitle

        return null
    }

    /**
     * Infers the workflow type from extracted parameters.
     */
    private fun inferWorkflowType(params: List<ExtractedParameter>): WorkflowTemplateType {
        val classTypes = params.map { it.nodeClassType }.toSet()
        return when {
            classTypes.any { it.contains("Inpaint", ignoreCase = true) } ->
                WorkflowTemplateType.INPAINTING
            classTypes.any { it.contains("Upscale", ignoreCase = true) } ->
                WorkflowTemplateType.UPSCALE
            classTypes.any { it.contains("LoraLoader", ignoreCase = true) } ->
                WorkflowTemplateType.LORA
            params.any { it.paramName == "image" || it.paramType == ParameterType.IMAGE } ->
                WorkflowTemplateType.IMG2IMG
            else -> WorkflowTemplateType.TXT2IMG
        }
    }

    private suspend fun parseImportDto(trimmed: String) {
        val dto = try {
            templateJson.decodeFromString<ImportDto>(trimmed)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            error("Invalid template JSON: ${e.message}")
        }
        val type = try {
            WorkflowTemplateType.valueOf(dto.type)
        } catch (_: IllegalArgumentException) {
            error("Unknown template type: ${dto.type}")
        }
        val category = try {
            WorkflowTemplateCategory.valueOf(dto.category)
        } catch (_: IllegalArgumentException) {
            WorkflowTemplateCategory.GENERAL
        }
        val template = WorkflowTemplate(
            id = 0L,
            name = dto.name,
            description = dto.description,
            type = type,
            category = category,
            variables = dto.variables.map { it.toModel() },
            isBuiltIn = false,
            version = dto.version,
            author = dto.author,
            createdAt = currentTimeMillis(),
        )
        repository.saveTemplate(template.toSavedPrompt())
    }

    companion object {
        private const val TAG = "ImportWorkflowTemplate"
    }
}

/**
 * Converts an [ExtractedParameter] to a [TemplateVariable] for storage.
 */
internal fun ExtractedParameter.toTemplateVariable(): TemplateVariable {
    val varType = when (paramType) {
        ParameterType.TEXT -> TemplateVariableType.TEXT
        ParameterType.NUMBER -> if (min != null || max != null) {
            TemplateVariableType.SLIDER
        } else {
            TemplateVariableType.NUMBER
        }
        ParameterType.SELECT -> TemplateVariableType.SELECT
        ParameterType.SEED -> TemplateVariableType.SEED
        ParameterType.IMAGE -> TemplateVariableType.IMAGE
        ParameterType.BOOLEAN -> TemplateVariableType.BOOLEAN
    }

    return TemplateVariable(
        name = "${nodeId}_$paramName",
        label = "$nodeTitle: $paramName",
        description = "",
        type = varType,
        defaultValue = currentValue,
        min = min,
        max = max,
        step = step,
        options = options,
        required = true,
    )
}
