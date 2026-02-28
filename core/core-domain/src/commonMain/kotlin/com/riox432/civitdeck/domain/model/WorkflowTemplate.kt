package com.riox432.civitdeck.domain.model

data class WorkflowTemplate(
    val id: Long,
    val name: String,
    val type: WorkflowTemplateType,
    val variables: List<TemplateVariable>,
    val isBuiltIn: Boolean,
    val createdAt: Long,
)

enum class WorkflowTemplateType {
    TXT2IMG,
    IMG2IMG,
    INPAINTING,
    UPSCALE,
}

data class TemplateVariable(
    val name: String,
    val type: TemplateVariableType,
    val defaultValue: String,
    val options: List<String> = emptyList(),
    val required: Boolean = true,
)

enum class TemplateVariableType {
    TEXT,
    NUMBER,
    SELECT,
}
