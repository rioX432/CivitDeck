package com.riox432.civitdeck.ui.dataset

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailSheet(
    image: DatasetImage,
    onTrainableToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        ImageDetailSheetContent(image = image, onTrainableToggle = onTrainableToggle)
    }
}

@Composable
private fun ImageDetailSheetContent(
    image: DatasetImage,
    onTrainableToggle: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = Spacing.lg)) {
        SourceBadgeRow(sourceType = image.sourceType)
        TrainableRow(trainable = image.trainable, onToggle = onTrainableToggle)
        val note = image.licenseNote
        if (!note.isNullOrEmpty()) {
            LicenseNoteRow(note = note)
        }
    }
}

@Composable
private fun SourceBadgeRow(sourceType: ImageSource) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Source",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        SourceBadge(sourceType = sourceType)
    }
}

@Composable
private fun SourceBadge(sourceType: ImageSource) {
    val (label, containerColor) = when (sourceType) {
        ImageSource.CIVITAI -> "CivitAI" to MaterialTheme.colorScheme.primaryContainer
        ImageSource.LOCAL -> "Local" to MaterialTheme.colorScheme.secondaryContainer
        ImageSource.GENERATED -> "Generated" to MaterialTheme.colorScheme.tertiaryContainer
    }
    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(containerColor = containerColor),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(CornerRadius.chip),
    )
}

@Composable
private fun TrainableRow(trainable: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Include in training",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Switch(
            checked = trainable,
            onCheckedChange = onToggle,
        )
    }
}

@Composable
private fun LicenseNoteRow(note: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Text(
            text = "License",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}
