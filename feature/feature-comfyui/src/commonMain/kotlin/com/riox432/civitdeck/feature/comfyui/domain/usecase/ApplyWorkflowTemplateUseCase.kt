package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.WorkflowTemplate

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
