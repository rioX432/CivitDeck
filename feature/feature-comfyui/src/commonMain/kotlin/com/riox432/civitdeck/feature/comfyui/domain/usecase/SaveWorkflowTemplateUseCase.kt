package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import com.riox432.civitdeck.domain.util.currentTimeMillis

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
