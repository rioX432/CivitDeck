package com.riox432.civitdeck.ui.dataset

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.riox432.civitdeck.ui.theme.Spacing

private const val MAX_CAPTION_LENGTH = 2000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptionEditorSheet(
    imageId: Long,
    initialCaption: String,
    onSave: (Long, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var captionText by remember { mutableStateOf(initialCaption) }

    ModalBottomSheet(
        onDismissRequest = {
            onSave(imageId, captionText)
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        CaptionEditorContent(
            captionText = captionText,
            onTextChange = { captionText = it },
        )
    }
}

@Composable
private fun CaptionEditorContent(
    captionText: String,
    onTextChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg)
            .padding(bottom = Spacing.lg),
    ) {
        Text(
            text = "Edit Caption",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = Spacing.md),
        )
        OutlinedTextField(
            value = captionText,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Caption") },
            minLines = 4,
            maxLines = 8,
            placeholder = { Text("Describe this image for training…") },
        )
        Text(
            text = "${captionText.length} / $MAX_CAPTION_LENGTH",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xs),
        )
    }
}
