package com.riox432.civitdeck.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    if (showDivider) {
        HorizontalDivider(modifier = Modifier.padding(bottom = Spacing.sm))
    }
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier,
    )
}
