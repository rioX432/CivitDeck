package com.riox432.civitdeck.ui.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.ShareHashtag
import com.riox432.civitdeck.ui.theme.Spacing

private const val X_CHAR_LIMIT = 280

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SocialShareSheet(
    hashtags: List<ShareHashtag>,
    onToggleHashtag: (String, Boolean) -> Unit,
    onAddHashtag: (String) -> Unit,
    onRemoveHashtag: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var caption by remember { mutableStateOf("") }
    var newTagInput by remember { mutableStateOf("") }

    val enabledTags = hashtags.filter { it.isEnabled }.map { it.tag }
    val hashtagText = enabledTags.joinToString(" ")
    val fullText = buildShareText(caption, hashtagText)
    val charCount = fullText.length

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
        ) {
            Text("Share", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(Spacing.md))
            CaptionField(caption = caption, onCaptionChange = { caption = it })
            Spacer(modifier = Modifier.height(Spacing.sm))
            CharCounter(count = charCount)
            Spacer(modifier = Modifier.height(Spacing.md))
            HashtagSection(
                hashtags = hashtags,
                onToggle = onToggleHashtag,
                onRemove = onRemoveHashtag,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            AddTagRow(
                input = newTagInput,
                onInputChange = { newTagInput = it },
                onAdd = {
                    if (newTagInput.isNotBlank()) {
                        onAddHashtag(newTagInput)
                        newTagInput = ""
                    }
                },
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            ActionButtons(
                context = context,
                fullText = fullText,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun CaptionField(caption: String, onCaptionChange: (String) -> Unit) {
    OutlinedTextField(
        value = caption,
        onValueChange = onCaptionChange,
        label = { Text("Caption") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 5,
    )
}

@Composable
private fun CharCounter(count: Int) {
    val color = when {
        count > X_CHAR_LIMIT -> MaterialTheme.colorScheme.error
        count > (X_CHAR_LIMIT * 0.9).toInt() -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = "$count / $X_CHAR_LIMIT",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HashtagSection(
    hashtags: List<ShareHashtag>,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
) {
    Text("Hashtags", style = MaterialTheme.typography.labelMedium)
    Spacer(modifier = Modifier.height(Spacing.xs))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        hashtags.forEach { hashtag ->
            FilterChip(
                selected = hashtag.isEnabled,
                onClick = { onToggle(hashtag.tag, !hashtag.isEnabled) },
                label = { Text(hashtag.tag, style = MaterialTheme.typography.labelSmall) },
                trailingIcon = if (hashtag.isCustom) {
                    {
                        IconButton(
                            onClick = { onRemove(hashtag.tag) },
                            modifier = Modifier.size(16.dp),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                        }
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun AddTagRow(
    input: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            label = { Text("Add tag") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = "Add hashtag")
        }
    }
}

@Composable
private fun ActionButtons(
    context: Context,
    fullText: String,
    onDismiss: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedButton(
            onClick = {
                copyToClipboard(context, fullText)
                onDismiss()
            },
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text("Copy")
        }
        Button(
            onClick = {
                shareText(context, fullText)
                onDismiss()
            },
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text("Share")
        }
    }
}

private fun buildShareText(caption: String, hashtagText: String): String {
    return listOf(caption.trim(), hashtagText)
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Share text", text))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}
