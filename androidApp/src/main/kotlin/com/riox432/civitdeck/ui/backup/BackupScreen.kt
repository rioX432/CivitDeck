package com.riox432.civitdeck.ui.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.BackupCategory
import com.riox432.civitdeck.domain.model.RestoreStrategy
import com.riox432.civitdeck.feature.settings.presentation.BackupUiState
import com.riox432.civitdeck.feature.settings.presentation.BackupViewModel
import com.riox432.civitdeck.ui.theme.Spacing
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { readFileContent(context, it) }?.let { viewModel.onImportFileLoaded(it) }
    }

    BackupSideEffects(state, context, snackbarHostState, viewModel)

    Scaffold(
        topBar = { BackupTopBar(onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        BackupContent(padding, state, viewModel, importLauncher)
    }

    if (state.showImportConfirmation) {
        ImportConfirmationDialog(
            state = state,
            onStrategyChanged = viewModel::onRestoreStrategyChanged,
            onToggleCategory = viewModel::onToggleCategory,
            onConfirm = viewModel::onConfirmImport,
            onDismiss = viewModel::onDismissImportConfirmation,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text("Backup & Restore") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
    )
}

@Composable
private fun BackupSideEffects(
    state: BackupUiState,
    context: Context,
    snackbarHostState: SnackbarHostState,
    viewModel: BackupViewModel,
) {
    LaunchedEffect(state.exportedJson) {
        state.exportedJson?.let { json ->
            shareBackupFile(context, json)
            viewModel.onExportHandled()
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageDismissed()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorDismissed()
        }
    }
}

@Composable
private fun BackupContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    state: BackupUiState,
    viewModel: BackupViewModel,
    importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
    ) {
        item { SectionTitle("Select Data") }
        item { SelectionControls(viewModel) }
        items(BackupCategory.entries.toList(), key = { it.name }) { category ->
            CategoryRow(
                category = category,
                isSelected = category in state.selectedCategories,
                onToggle = { viewModel.onToggleCategory(category) },
            )
        }
        item { HorizontalDivider() }
        item { Spacer(Modifier.height(Spacing.md)) }
        item { ActionButtons(state, viewModel, importLauncher) }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}

@Composable
private fun SelectionControls(viewModel: BackupViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        TextButton(onClick = viewModel::onSelectAll) { Text("Select All") }
        TextButton(onClick = viewModel::onDeselectAll) { Text("Deselect All") }
    }
}

@Composable
private fun CategoryRow(
    category: BackupCategory,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            .clickable(onClickLabel = "Toggle selection", onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = Spacing.sm),
        )
    }
}

@Composable
private fun ActionButtons(
    state: BackupUiState,
    viewModel: BackupViewModel,
    importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Button(
            onClick = viewModel::onExport,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.selectedCategories.isNotEmpty() && !state.isExporting,
        ) {
            if (state.isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = Spacing.sm),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text("Export Backup")
        }
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isImporting,
        ) {
            if (state.isImporting) {
                CircularProgressIndicator(modifier = Modifier.padding(end = Spacing.sm))
            }
            Text("Import from File")
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun ImportConfirmationDialog(
    state: BackupUiState,
    onStrategyChanged: (RestoreStrategy) -> Unit,
    onToggleCategory: (BackupCategory) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Backup") },
        text = { ImportDialogContent(state, onStrategyChanged, onToggleCategory) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = state.selectedCategories.isNotEmpty(),
            ) { Text("Restore") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ImportDialogContent(
    state: BackupUiState,
    onStrategyChanged: (RestoreStrategy) -> Unit,
    onToggleCategory: (BackupCategory) -> Unit,
) {
    Column {
        Text(
            "Found data for ${state.importCategories.size} categories.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(Spacing.md))
        Text("Restore Strategy:", style = MaterialTheme.typography.titleSmall)
        RestoreStrategy.entries.forEach { strategy ->
            key(strategy.name) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs)
                        .clickable(onClickLabel = "Select strategy") { onStrategyChanged(strategy) },
                ) {
                    RadioButton(
                        selected = state.restoreStrategy == strategy,
                        onClick = { onStrategyChanged(strategy) },
                    )
                    Text(strategy.displayName, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text("Categories to restore:", style = MaterialTheme.typography.titleSmall)
        state.importCategories.forEach { category ->
            key(category.name) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs)
                        .clickable { onToggleCategory(category) },
                ) {
                    Checkbox(
                        checked = category in state.selectedCategories,
                        onCheckedChange = { onToggleCategory(category) },
                    )
                    Text(category.displayName, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun readFileContent(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
        null
    }
}

private fun shareBackupFile(context: Context, json: String) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val fileName = "civitdeck_backup_$timestamp.json"
    val file = File(context.cacheDir, "backups").also { it.mkdirs() }.resolve(fileName)
    file.writeText(json)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export Backup"))
}
