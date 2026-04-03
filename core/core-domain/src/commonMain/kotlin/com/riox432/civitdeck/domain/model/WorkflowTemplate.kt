package com.riox432.civitdeck.domain.model

data class WorkflowTemplate(
    val id: Long,
    val name: String,
    val description: String = "",
    val type: WorkflowTemplateType,
    val category: WorkflowTemplateCategory = WorkflowTemplateCategory.GENERAL,
    val variables: List<TemplateVariable>,
    val isBuiltIn: Boolean,
    val version: Int = 1,
    val author: String = "",
    val createdAt: Long,
)

enum class WorkflowTemplateType {
    TXT2IMG,
    IMG2IMG,
    INPAINTING,
    UPSCALE,
    LORA,
}

enum class WorkflowTemplateCategory {
    GENERAL,
    ANIME,
    PHOTOREALISTIC,
    ARTISTIC,
    UTILITY,
}

data class TemplateVariable(
    val name: String,
    val label: String = "",
    val description: String = "",
    val type: TemplateVariableType,
    val defaultValue: String,
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null,
    val options: List<String> = emptyList(),
    val required: Boolean = true,
)

enum class TemplateVariableType {
    TEXT,
    NUMBER,
    SELECT,
    SLIDER,
}
