@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.HapticFeedbackType
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.ui.components.rememberHapticFeedback
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import com.riox432.civitdeck.ui.theme.Spacing

internal val filterTypes = listOf(null) + listOf(
    ModelType.Checkpoint,
    ModelType.LORA,
    ModelType.LoCon,
    ModelType.Controlnet,
    ModelType.TextualInversion,
    ModelType.Hypernetwork,
    ModelType.Upscaler,
    ModelType.VAE,
    ModelType.Poses,
    ModelType.Wildcards,
    ModelType.Workflows,
    ModelType.MotionModule,
    ModelType.AestheticGradient,
    ModelType.Other,
)

internal fun ModelType.displayLabel(): String = when (this) {
    ModelType.TextualInversion -> "Textual Inv."
    ModelType.AestheticGradient -> "Aesthetic Grad."
    ModelType.MotionModule -> "Motion Module"
    else -> name
}

internal fun SortOrder.displayLabel(): String = when (this) {
    SortOrder.HighestRated -> "Highest Rated"
    SortOrder.MostDownloaded -> "Most Downloaded"
    else -> name
}

internal fun TimePeriod.displayLabel(): String = when (this) {
    TimePeriod.AllTime -> "All Time"
    else -> name
}

@Composable
internal fun FilterSectionHeader(title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    showCheckmark: Boolean = false,
) {
    val haptic = rememberHapticFeedback()
    val chipColorTween = tween<Color>(Duration.fast, easing = Easing.standard)
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = chipColorTween,
        label = "chipBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = chipColorTween,
        label = "chipText",
    )
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(RoundedCornerShape(CornerRadius.chip))
            .background(backgroundColor)
            .clickable(
                onClickLabel = "Toggle filter",
                onClick = {
                    haptic(HapticFeedbackType.Selection)
                    onClick()
                },
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (showCheckmark && isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = textColor,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            },
            color = textColor,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagFilterSection(
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    placeholder: String,
    header: String? = null,
    headerSubtitle: String? = null,
    chipBackground: @Composable () -> Color,
    chipForeground: @Composable () -> Color,
) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (header != null) {
            FilterSectionHeader(header, headerSubtitle)
        }
        TagInputRow(placeholder = placeholder, onAddTag = onAddTag)
        if (tags.isNotEmpty()) {
            val bg = chipBackground()
            val fg = chipForeground()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                tags.forEach { tag ->
                    TagChip(tag = tag, onRemove = { onRemoveTag(tag) }, background = bg, foreground = fg)
                }
            }
        }
    }
}

@Composable
private fun TagInputRow(placeholder: String, onAddTag: (String) -> Unit) {
    var tagInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = tagInput,
            onValueChange = { tagInput = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (tagInput.isNotBlank()) {
                        onAddTag(tagInput)
                        tagInput = ""
                        keyboardController?.hide()
                    }
                },
            ),
        )
        IconButton(
            onClick = {
                if (tagInput.isNotBlank()) {
                    onAddTag(tagInput)
                    tagInput = ""
                    keyboardController?.hide()
                }
            },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add tag")
        }
    }
}

@Composable
private fun TagChip(
    tag: String,
    onRemove: () -> Unit,
    background: Color,
    foreground: Color,
) {
    Row(
        modifier = Modifier
            .background(background, RoundedCornerShape(CornerRadius.chip))
            .padding(start = Spacing.sm, end = Spacing.xs)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(text = tag, style = MaterialTheme.typography.labelSmall, color = foreground)
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove $tag",
            modifier = Modifier.size(14.dp).clickable(onClick = onRemove, onClickLabel = "Remove tag"),
            tint = foreground,
        )
    }
}
