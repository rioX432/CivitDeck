package com.riox432.civitdeck.ui.components.comfyui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.ui.theme.Spacing

/**
 * Positive and negative prompt text fields for image generation.
 *
 * @param prompt Current positive prompt text
 * @param negativePrompt Current negative prompt text
 * @param onPromptChanged Callback when positive prompt changes
 * @param onNegativePromptChanged Callback when negative prompt changes
 * @param promptLabel Label for the positive prompt field
 * @param negativePromptLabel Label for the negative prompt field
 * @param modifier Optional modifier for the column
 */
@Composable
fun PromptInputFields(
    prompt: String,
    negativePrompt: String,
    onPromptChanged: (String) -> Unit,
    onNegativePromptChanged: (String) -> Unit,
    promptLabel: String = "Prompt",
    negativePromptLabel: String = "Negative Prompt",
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChanged,
            label = { Text(promptLabel) },
            modifier = Modifier.fillMaxWidth(),
            minLines = PROMPT_MIN_LINES,
            maxLines = PROMPT_MAX_LINES,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = negativePrompt,
            onValueChange = onNegativePromptChanged,
            label = { Text(negativePromptLabel) },
            modifier = Modifier.fillMaxWidth(),
            minLines = NEGATIVE_PROMPT_MIN_LINES,
            maxLines = NEGATIVE_PROMPT_MAX_LINES,
        )
    }
}

private const val PROMPT_MIN_LINES = 3
private const val PROMPT_MAX_LINES = 6
private const val NEGATIVE_PROMPT_MIN_LINES = 2
private const val NEGATIVE_PROMPT_MAX_LINES = 4
