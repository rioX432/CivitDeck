package com.riox432.civitdeck.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.riox432.civitdeck.ui.theme.Spacing

/**
 * A horizontally scrollable row of [FilterChip]s for selecting a single value
 * from [options].
 *
 * The row must scroll rather than clip: in a plain Row the first chip that
 * doesn't fully fit is measured with the leftover width, its label wraps one
 * character per line, and the whole row inflates vertically (seen as a huge
 * blank area under model-version chips).
 *
 * @param options The list of selectable values.
 * @param selected The currently selected value.
 * @param onSelect Called when the user taps a chip.
 * @param label Maps each option to its display string.
 */
@Composable
fun <T> FilterChipRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        label(option),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}
