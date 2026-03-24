package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun AccentColorRow(
    selected: AccentColor,
    onChanged: (AccentColor) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Text("Accent Color", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(Spacing.sm))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(AccentColor.entries.toList(), key = { it.name }) { color ->
                AccentColorSwatch(color = color, isSelected = color == selected) {
                    onChanged(color)
                }
            }
        }
    }
}

@Composable
internal fun AccentColorSwatch(
    color: AccentColor,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    // Intentional: seedHex is the user-chosen accent seed color, used here as a literal preview
    // swatch. It cannot be expressed via MaterialTheme tokens because it is the input to the
    // dynamic color pipeline, not a derived token value.
    @Suppress("MagicNumber")
    val swatchColor = Color(color.seedHex)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(swatchColor)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick, onClickLabel = "Select color theme"),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.cd_selected),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun AmoledDarkModeRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("AMOLED Dark Mode", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Use pure black background in dark mode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
internal fun GridColumnsRow(columns: Int, onChanged: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Grid Columns", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(2, 3).forEach { count ->
                TextButton(onClick = { onChanged(count) }) {
                    Text(
                        text = count.toString(),
                        color = if (columns == count) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
