package com.riox432.civitdeck.ui.detail

import android.text.Html
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.ui.components.ExpandableTextSection
import com.riox432.civitdeck.ui.components.SectionHeader
import com.riox432.civitdeck.ui.theme.Spacing

internal const val DESCRIPTION_COLLAPSED_LINES = 4

@Composable
internal fun ModelHeader(model: Model, onCreatorClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
        Text(
            text = model.name,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = model.type.name,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
            )
            if (model.creator != null) {
                Text(
                    text = "by ${model.creator!!.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClickLabel = "View creator profile") {
                        onCreatorClick(model.creator!!.username)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagsSection(tags: List<String>) {
    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            tags.forEach { tag ->
                SuggestionChip(
                    onClick = {},
                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

@Composable
internal fun DescriptionSection(description: String) {
    val plainText = remember(description) {
        Html.fromHtml(description, Html.FROM_HTML_MODE_COMPACT).toString()
    }
    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        SectionHeader(title = "Description", showDivider = true)
        Spacer(modifier = Modifier.height(Spacing.sm))
        ExpandableTextSection(
            text = plainText,
            collapsedMaxLines = DESCRIPTION_COLLAPSED_LINES,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ImageActionsRow(
    onViewImages: () -> Unit,
    showTryInComfyUI: Boolean = false,
    onTryInComfyUI: () -> Unit = {},
    onSendToPC: () -> Unit = {},
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Button(
            onClick = onViewImages,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Text("View Community Images")
        }
        OutlinedButton(onClick = onSendToPC) {
            Text("Send to PC")
        }
        if (showTryInComfyUI) {
            Button(
                onClick = onTryInComfyUI,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            ) {
                Text("Try in ComfyUI")
            }
        }
    }
}
