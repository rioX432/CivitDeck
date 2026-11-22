package com.riox432.civitdeck.ui.prompts

import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.model.SourceProvider
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.domain.model.SourceReferenceDecodeResult
import kotlin.test.Test
import kotlin.test.assertEquals

class PromptRowStateTest {

    @Test
    fun a_blank_prompt_source_row_is_a_reference_without_prompt_actions() {
        val row = promptRowState(prompt(text = "", source = DECODED))

        assertEquals(PromptRowState(isReference = true, promptActionsEnabled = false, hasSource = true), row)
    }

    @Test
    fun a_source_row_with_a_prompt_keeps_its_prompt_actions_and_source() {
        val row = promptRowState(prompt(text = "a lighthouse", source = DECODED))

        assertEquals(PromptRowState(isReference = false, promptActionsEnabled = true, hasSource = true), row)
    }

    @Test
    fun an_ordinary_prompt_or_template_is_unchanged() {
        val row = promptRowState(prompt(text = "a {subject} at dusk", source = null))

        assertEquals(PromptRowState(isReference = false, promptActionsEnabled = true, hasSource = false), row)
    }

    @Test
    fun a_blank_built_in_template_without_a_source_keeps_its_actions() {
        val row = promptRowState(prompt(text = "", source = null))

        assertEquals(PromptRowState(isReference = false, promptActionsEnabled = true, hasSource = false), row)
    }

    @Test
    fun an_unreadable_snapshot_still_offers_source_inspection() {
        val row = promptRowState(prompt(text = "", source = SourceReferenceDecodeResult.Malformed))

        assertEquals(PromptRowState(isReference = true, promptActionsEnabled = false, hasSource = true), row)
    }

    private fun prompt(text: String, source: SourceReferenceDecodeResult?) = SavedPrompt(
        id = 1L,
        prompt = text,
        negativePrompt = null,
        sampler = null,
        steps = null,
        cfgScale = null,
        seed = null,
        modelName = null,
        size = null,
        sourceImageUrl = null,
        savedAt = 0L,
        sourceReference = source,
    )

    private companion object {
        val DECODED = SourceReferenceDecodeResult.Decoded(
            SourceReference(
                provider = SourceProvider.Civitai,
                artworkId = "101",
                artworkUrl = "https://civitai.com/images/101",
            ),
        )
    }
}
