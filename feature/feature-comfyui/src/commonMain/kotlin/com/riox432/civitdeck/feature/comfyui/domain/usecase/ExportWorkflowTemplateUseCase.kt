package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.WorkflowTemplate
import kotlinx.serialization.Serializable

class ExportWorkflowTemplateUseCase {
    @Serializable
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
        return prettyJson.encodeToString(dto)
    }
}
