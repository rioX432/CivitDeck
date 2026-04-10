package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.model.TemplateVariable
import com.riox432.civitdeck.domain.model.TemplateVariableType
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.model.WorkflowTemplateCategory
import com.riox432.civitdeck.domain.model.WorkflowTemplateType
import com.riox432.civitdeck.util.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "WorkflowTemplate"

internal val templateJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal val prettyJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

// -- Serializable DTOs for variable schema stored in DB --

@Serializable
internal data class TemplateVariableDto(
    val name: String,
    val label: String = "",
    val description: String = "",
    val type: String,
    val defaultValue: String,
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null,
    val options: List<String> = emptyList(),
    val required: Boolean = true,
)

@Serializable
internal data class TemplateMetadataDto(
    val description: String = "",
    val category: String = "GENERAL",
    val version: Int = 1,
    val author: String = "",
)

// -- Mappers --

internal fun TemplateVariable.toDto() = TemplateVariableDto(
    name = name,
    label = label,
    description = description,
    type = type.name,
    defaultValue = defaultValue,
    min = min,
    max = max,
    step = step,
    options = options,
    required = required,
)

internal fun TemplateVariableDto.toModel() = TemplateVariable(
    name = name,
    label = label,
    description = description,
    type = try { TemplateVariableType.valueOf(type) } catch (_: IllegalArgumentException) {
        TemplateVariableType.TEXT
    },
    defaultValue = defaultValue,
    min = min,
    max = max,
    step = step,
    options = options,
    required = required,
)

internal fun WorkflowTemplate.toSavedPrompt(): SavedPrompt {
    val variablesJson = templateJson.encodeToString(variables.map { it.toDto() })
    val metadataJson = templateJson.encodeToString(
        TemplateMetadataDto(
            description = description,
            category = category.name,
            version = version,
            author = author,
        ),
    )
    return SavedPrompt(
        id = if (id <= 0L) 0L else id,
        prompt = variables.firstOrNull { it.name == "positive_prompt" }?.defaultValue ?: "",
        negativePrompt = variables.firstOrNull { it.name == "negative_prompt" }?.defaultValue,
        sampler = null,
        steps = null,
        cfgScale = null,
        seed = null,
        modelName = null,
        size = null,
        sourceImageUrl = null,
        savedAt = createdAt,
        isTemplate = true,
        templateName = name,
        autoSaved = false,
        templateVariables = variablesJson,
        templateType = type.name,
        templateMetadata = metadataJson,
    )
}

internal fun SavedPrompt.toWorkflowTemplate(): WorkflowTemplate? {
    val type = try {
        WorkflowTemplateType.valueOf(templateType ?: return null)
    } catch (_: IllegalArgumentException) {
        return null
    }
    val vars = try {
        templateJson.decodeFromString<List<TemplateVariableDto>>(templateVariables ?: "[]")
            .map { it.toModel() }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Logger.w(TAG, "Failed to parse template variables: ${e.message}")
        emptyList()
    }
    val metadata = try {
        templateJson.decodeFromString<TemplateMetadataDto>(templateMetadata ?: "{}")
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Logger.w(TAG, "Failed to parse template metadata: ${e.message}")
        TemplateMetadataDto()
    }
    return WorkflowTemplate(
        id = id,
        name = templateName ?: "Unnamed",
        description = metadata.description,
        type = type,
        category = try {
            WorkflowTemplateCategory.valueOf(metadata.category)
        } catch (_: IllegalArgumentException) {
            WorkflowTemplateCategory.GENERAL
        },
        variables = vars,
        isBuiltIn = id < 0L,
        version = metadata.version,
        author = metadata.author,
        createdAt = savedAt,
    )
}
