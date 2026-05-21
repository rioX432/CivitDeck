package com.riox432.civitdeck.ui.components.comfyui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private const val RANDOM_SEED = -1L

/**
 * Text field for seed input with -1 representing a random seed.
 *
 * When the seed is -1, the field is displayed as empty. Invalid input
 * (non-numeric) falls back to -1 (random).
 *
 * @param seed Current seed value (-1 for random)
 * @param onSeedChanged Callback when the seed changes
 * @param label Label for the text field
 * @param modifier Optional modifier for the field
 */
@Composable
fun SeedInputField(
    seed: Long,
    onSeedChanged: (Long) -> Unit,
    label: String = "Seed (-1 = random)",
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = if (seed == RANDOM_SEED) "" else seed.toString(),
        onValueChange = { onSeedChanged(it.toLongOrNull() ?: RANDOM_SEED) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
    )
}
