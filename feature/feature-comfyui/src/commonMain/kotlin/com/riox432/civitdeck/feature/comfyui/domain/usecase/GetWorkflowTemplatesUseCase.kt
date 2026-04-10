package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetWorkflowTemplatesUseCase(private val repository: SavedPromptRepository) {
    operator fun invoke(): Flow<List<WorkflowTemplate>> =
        repository.observeTemplates().map { prompts ->
            prompts.mapNotNull { it.toWorkflowTemplate() }
                .sortedWith(compareByDescending<WorkflowTemplate> { it.isBuiltIn }.thenByDescending { it.createdAt })
        }
}
