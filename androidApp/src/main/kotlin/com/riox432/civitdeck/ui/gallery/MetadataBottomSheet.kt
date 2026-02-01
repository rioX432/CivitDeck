package com.riox432.civitdeck.ui.gallery

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.ImageGenerationMeta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataBottomSheet(
    meta: ImageGenerationMeta,
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        MetadataContent(meta = meta, context = context)
    }
}

@Composable
private fun MetadataContent(
    meta: ImageGenerationMeta,
    context: Context,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = "Generation Info",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))

        PromptSection(meta = meta, context = context)
        MetadataParams(meta = meta)
    }
}

@Composable
private fun PromptSection(
    meta: ImageGenerationMeta,
    context: Context,
) {
    meta.prompt?.let { prompt ->
        MetadataLabel("Prompt")
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { copyToClipboard(context, "Prompt", prompt) },
        ) {
            Text("Copy Prompt")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    meta.negativePrompt?.let { negPrompt ->
        MetadataLabel("Negative Prompt")
        Text(
            text = negPrompt,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
    if (meta.prompt != null || meta.negativePrompt != null) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun MetadataParams(meta: ImageGenerationMeta) {
    meta.model?.let { MetadataRow("Model", it) }
    meta.sampler?.let { MetadataRow("Sampler", it) }
    meta.steps?.let { MetadataRow("Steps", it.toString()) }
    meta.cfgScale?.let { MetadataRow("CFG Scale", it.toString()) }
    meta.seed?.let { MetadataRow("Seed", it.toString()) }
    meta.size?.let { MetadataRow("Size", it) }
}

@Composable
private fun MetadataLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}
