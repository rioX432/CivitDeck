package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.model.WorkflowTemplateCategory
import com.riox432.civitdeck.domain.model.WorkflowTemplateType
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import com.riox432.civitdeck.domain.util.currentTimeMillis
import kotlinx.serialization.Serializable

class ImportWorkflowTemplateUseCase(private val repository: SavedPromptRepository) {
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
