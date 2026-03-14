package com.riox432.civitdeck.ui.backup

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.BackupCategory
import com.riox432.civitdeck.domain.model.RestoreStrategy
import com.riox432.civitdeck.ui.theme.Spacing
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun DesktopBackupScreen(
    viewModel: DesktopBackupViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarMessage = remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    LaunchedEffect(state.exportedJson) {
        state.exportedJson?.let { json ->
            saveBackupToFile(json)
            viewModel.onExportHandled()
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarMessage.value = it
            viewModel.onMessageDismissed()
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column {
            BackupToolbar(onBack = onBack)
            BackupBody(state = state, viewModel = viewModel)
        }
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

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::onErrorDismissed,
            confirmButton = { TextButton(onClick = viewModel::onErrorDismissed) { Text("OK") } },
            title = { Text("Error") },
            text = { Text(error) },
        )
    }
}

@Composable
private fun BackupToolbar(onBack: () -> Unit) {
    Surface(tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Backup & Restore",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
    }
}

@Composable
private fun BackupBody(
    state: BackupUiState,
    viewModel: DesktopBackupViewModel,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = "Select Data",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                TextButton(onClick = viewModel::onSelectAll) { Text("Select All") }
                TextButton(onClick = viewModel::onDeselectAll) { Text("Deselect All") }
            }
        }
        items(BackupCategory.entries.toList(), key = { it.name }) { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                    .clickable(onClickLabel = "Toggle") { viewModel.onToggleCategory(category) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = category in state.selectedCategories,
                    onCheckedChange = { viewModel.onToggleCategory(category) },
                )
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
        item { HorizontalDivider() }
        item { Spacer(Modifier.height(Spacing.md)) }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
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
                    onClick = { loadBackupFromFile(viewModel) },
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
        text = {
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
                                .clickable { onStrategyChanged(strategy) },
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
        },
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

private fun saveBackupToFile(json: String) {
    SwingUtilities.invokeLater {
        val chooser = JFileChooser().apply {
            dialogTitle = "Save Backup"
            fileFilter = FileNameExtensionFilter("JSON files", "json")
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            selectedFile = File("civitdeck_backup_$timestamp.json")
        }
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            var file = chooser.selectedFile
            if (!file.name.endsWith(".json")) {
                file = File(file.absolutePath + ".json")
            }
            file.writeText(json)
        }
    }
}

private fun loadBackupFromFile(viewModel: DesktopBackupViewModel) {
    SwingUtilities.invokeLater {
        val chooser = JFileChooser().apply {
            dialogTitle = "Open Backup File"
            fileFilter = FileNameExtensionFilter("JSON files", "json")
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            val json = chooser.selectedFile.readText()
            viewModel.onImportFileLoaded(json)
        }
    }
}
