@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.model.TemplateVariable
import com.riox432.civitdeck.domain.model.TemplateVariableType
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.model.WorkflowTemplateCategory
import com.riox432.civitdeck.domain.model.WorkflowTemplateType
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import com.riox432.civitdeck.domain.util.currentTimeMillis
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "WorkflowTemplate"

private val templateJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// -- Serializable DTO for variable schema stored in DB --

@kotlinx.serialization.Serializable
private data class TemplateVariableDto(
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

@kotlinx.serialization.Serializable
private data class TemplateMetadataDto(
    val description: String = "",
    val category: String = "GENERAL",
    val version: Int = 1,
    val author: String = "",
)

// -- Mappers --

private fun TemplateVariable.toDto() = TemplateVariableDto(
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

private fun TemplateVariableDto.toModel() = TemplateVariable(
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

private fun WorkflowTemplate.toSavedPrompt(): SavedPrompt {
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

private fun SavedPrompt.toWorkflowTemplate(): WorkflowTemplate? {
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

// -- Use cases --

class GetWorkflowTemplatesUseCase(private val repository: SavedPromptRepository) {
    operator fun invoke(): Flow<List<WorkflowTemplate>> =
        repository.observeTemplates().map { prompts ->
            prompts.mapNotNull { it.toWorkflowTemplate() }
                .sortedWith(compareByDescending<WorkflowTemplate> { it.isBuiltIn }.thenByDescending { it.createdAt })
        }
}

class SaveWorkflowTemplateUseCase(private val repository: SavedPromptRepository) {
    suspend operator fun invoke(template: WorkflowTemplate) {
        val withTime = if (template.createdAt == 0L) {
            template.copy(createdAt = currentTimeMillis())
        } else {
            template
        }
        repository.saveTemplate(withTime.toSavedPrompt())
    }
}

class DeleteWorkflowTemplateUseCase(private val repository: SavedPromptRepository) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}

class ExportWorkflowTemplateUseCase {
    @kotlinx.serialization.Serializable
    private data class ExportDto(
        val name: String,
        val description: String = "",
        val type: String,
        val category: String = "GENERAL",
        val version: Int = 1,
        val author: String = "",
        val variables: List<TemplateVariableDto>,
    )

    operator fun invoke(template: WorkflowTemplate): String {
        val dto = ExportDto(
            name = template.name,
            description = template.description,
            type = template.type.name,
            category = template.category.name,
            version = template.version,
            author = template.author,
            variables = template.variables.map { it.toDto() },
        )
        return Json { prettyPrint = true }.encodeToString(dto)
    }
}

class ImportWorkflowTemplateUseCase(private val repository: SavedPromptRepository) {
    @kotlinx.serialization.Serializable
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
        val dto = try {
            templateJson.decodeFromString<ImportDto>(trimmed)
        } catch (e: Exception) {
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
}

class ApplyWorkflowTemplateUseCase {
    /**
     * Substitutes the given variable values into the template and returns a partially filled
     * [ComfyUIGenerationParams]. Fields not covered by the template stay at their defaults.
     */
    operator fun invoke(
        template: WorkflowTemplate,
        values: Map<String, String>,
    ): ComfyUIGenerationParams {
        fun value(name: String) = values[name]
            ?: template.variables.firstOrNull { it.name == name }?.defaultValue
            ?: ""

        return ComfyUIGenerationParams(
            checkpoint = value("checkpoint"),
            prompt = value("positive_prompt"),
            negativePrompt = value("negative_prompt"),
            steps = value("steps").toIntOrNull() ?: ComfyUIGenerationParams.DEFAULT_STEPS,
            cfgScale = value("cfg").toDoubleOrNull() ?: ComfyUIGenerationParams.DEFAULT_CFG,
            seed = value("seed").toLongOrNull() ?: -1L,
            width = value("width").toIntOrNull() ?: ComfyUIGenerationParams.DEFAULT_DIMENSION,
            height = value("height").toIntOrNull() ?: ComfyUIGenerationParams.DEFAULT_DIMENSION,
        )
    }
}
