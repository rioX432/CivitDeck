package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.model.TemplateVariable
import com.riox432.civitdeck.domain.model.TemplateVariableType
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.model.WorkflowTemplateType
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val templateJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// -- Serializable DTO for variable schema stored in DB --

@kotlinx.serialization.Serializable
private data class TemplateVariableDto(
    val name: String,
    val type: String,
    val defaultValue: String,
    val options: List<String> = emptyList(),
    val required: Boolean = true,
)

// -- Mappers --

private fun WorkflowTemplate.toSavedPrompt(): SavedPrompt {
    val variablesJson = templateJson.encodeToString(
        variables.map {
            TemplateVariableDto(
                name = it.name,
                type = it.type.name,
                defaultValue = it.defaultValue,
                options = it.options,
                required = it.required,
            )
        },
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
    )
}

private fun SavedPrompt.toWorkflowTemplate(): WorkflowTemplate? {
    val type = try {
        WorkflowTemplateType.valueOf(templateType ?: return null)
    } catch (_: IllegalArgumentException) {
        return null
    }
    val vars = try {
        val dtos = templateJson.decodeFromString<List<TemplateVariableDto>>(templateVariables ?: "[]")
        dtos.map {
            TemplateVariable(
                name = it.name,
                type = try { TemplateVariableType.valueOf(it.type) } catch (_: IllegalArgumentException) {
                    TemplateVariableType.TEXT
                },
                defaultValue = it.defaultValue,
                options = it.options,
                required = it.required,
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
    return WorkflowTemplate(
        id = id,
        name = templateName ?: "Unnamed",
        type = type,
        variables = vars,
        isBuiltIn = id < 0L,
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
        val type: String,
        val variables: List<TemplateVariableDto>,
    )

    operator fun invoke(template: WorkflowTemplate): String {
        val dto = ExportDto(
            name = template.name,
            type = template.type.name,
            variables = template.variables.map {
                TemplateVariableDto(
                    name = it.name,
                    type = it.type.name,
                    defaultValue = it.defaultValue,
                    options = it.options,
                    required = it.required,
                )
            },
        )
        return Json { prettyPrint = true }.encodeToString(dto)
    }
}

class ImportWorkflowTemplateUseCase(private val repository: SavedPromptRepository) {
    @kotlinx.serialization.Serializable
    private data class ImportDto(
        val name: String,
        val type: String,
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
        val variables = dto.variables.map {
            TemplateVariable(
                name = it.name,
                type = try { TemplateVariableType.valueOf(it.type) } catch (_: IllegalArgumentException) {
                    TemplateVariableType.TEXT
                },
                defaultValue = it.defaultValue,
                options = it.options,
                required = it.required,
            )
        }
        val template = WorkflowTemplate(
            id = 0L,
            name = dto.name,
            type = type,
            variables = variables,
            isBuiltIn = false,
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
