package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.TemplateVariable
import com.riox432.civitdeck.domain.model.TemplateVariableType
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.model.WorkflowTemplateType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [ApplyWorkflowTemplateUseCase]: value resolution precedence
 * (provided value > template default > empty) and numeric parsing with
 * fallback to the [ComfyUIGenerationParams] defaults.
 */
class ApplyWorkflowTemplateUseCaseTest {

    private val useCase = ApplyWorkflowTemplateUseCase()

    private fun variable(name: String, default: String) = TemplateVariable(
        name = name,
        type = TemplateVariableType.TEXT,
        defaultValue = default,
    )

    private fun template(vararg variables: TemplateVariable) = WorkflowTemplate(
        id = 1L,
        name = "T",
        type = WorkflowTemplateType.TXT2IMG,
        variables = variables.toList(),
        isBuiltIn = false,
        createdAt = 0L,
    )

    @Test
    fun provided_values_take_precedence_over_template_defaults() {
        val tpl = template(
            variable("positive_prompt", "default prompt"),
            variable("steps", "10"),
        )

        val params = useCase(
            tpl,
            mapOf("positive_prompt" to "user prompt", "steps" to "25"),
        )

        assertEquals("user prompt", params.prompt)
        assertEquals(25, params.steps)
    }

    @Test
    fun falls_back_to_template_default_when_value_not_provided() {
        val tpl = template(
            variable("positive_prompt", "default prompt"),
            variable("cfg", "8.5"),
        )

        val params = useCase(tpl, emptyMap())

        assertEquals("default prompt", params.prompt)
        assertEquals(8.5, params.cfgScale)
    }

    @Test
    fun uses_generation_param_defaults_when_numeric_value_is_unparseable() {
        val tpl = template(
            variable("steps", "not-a-number"),
            variable("cfg", "abc"),
            variable("width", ""),
            variable("seed", "xyz"),
        )

        val params = useCase(tpl, emptyMap())

        assertEquals(ComfyUIGenerationParams.DEFAULT_STEPS, params.steps)
        assertEquals(ComfyUIGenerationParams.DEFAULT_CFG, params.cfgScale)
        assertEquals(ComfyUIGenerationParams.DEFAULT_DIMENSION, params.width)
        // Seed has its own sentinel default of -1 (randomize) rather than a constant.
        assertEquals(-1L, params.seed)
    }

    @Test
    fun absent_variables_resolve_to_empty_string_for_text_fields() {
        // Template defines nothing; checkpoint/prompt are unfilled -> empty strings.
        val params = useCase(template(), emptyMap())

        assertEquals("", params.checkpoint)
        assertEquals("", params.prompt)
        assertEquals("", params.negativePrompt)
        // Numeric fields still get their type defaults.
        assertEquals(ComfyUIGenerationParams.DEFAULT_STEPS, params.steps)
    }
}
