package com.riox432.civitdeck.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.ThemeMode
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.plugin.ThemePlugin
import com.riox432.civitdeck.plugin.model.PluginState
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.usecase.ActivateThemePluginUseCase
import com.riox432.civitdeck.usecase.ImportThemeUseCase
import com.riox432.civitdeck.usecase.ObserveThemePluginsUseCase
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: DisplaySettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val themePlugins by koinInject<ObserveThemePluginsUseCase>()
        .invoke().collectAsStateWithLifecycle(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item { SectionHeader("Theme") }
            item { ThemeModeRow(state.themeMode, viewModel::onThemeModeChanged) }
            item { AccentColorRow(state.accentColor, viewModel::onAccentColorChanged) }
            item { AmoledDarkModeRow(state.amoledDarkMode, viewModel::onAmoledDarkModeChanged) }
            if (themePlugins.isNotEmpty()) {
                item { SectionHeader("Custom Themes") }
                item { BuiltInThemeRadio(themePlugins) }
                items(themePlugins, key = { it.manifest.id }) { plugin ->
                    CustomThemeRadio(plugin)
                }
            }
            item { ImportThemeRow() }
            item { SectionHeader("Display") }
            item { GridColumnsRow(state.gridColumns, viewModel::onGridColumnsChanged) }
        }
    }
}

@Composable
private fun BuiltInThemeRadio(themePlugins: List<ThemePlugin>) {
    val scope = rememberCoroutineScope()
    val activate = koinInject<ActivateThemePluginUseCase>()
    val noneActive = themePlugins.none { it.state == PluginState.ACTIVE }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = noneActive,
            onClick = { scope.launch { activate(null) } },
        )
        Text("Built-in (Accent Color)", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CustomThemeRadio(plugin: ThemePlugin) {
    val scope = rememberCoroutineScope()
    val activate = koinInject<ActivateThemePluginUseCase>()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = plugin.state == PluginState.ACTIVE,
            onClick = { scope.launch { activate(plugin.manifest.id) } },
        )
        Column {
            Text(plugin.manifest.name, style = MaterialTheme.typography.bodyLarge)
            if (plugin.manifest.author.isNotBlank()) {
                Text(
                    "by ${plugin.manifest.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ImportThemeRow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importTheme = koinInject<ImportThemeUseCase>()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readText() ?: return@launch
            importTheme(json)
                .onSuccess {
                    Toast.makeText(context, "Theme imported", Toast.LENGTH_SHORT).show()
                }
                .onFailure { e ->
                    Toast.makeText(
                        context,
                        "Import failed: ${e.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
        }
    }

    OutlinedButton(
        onClick = { launcher.launch("application/json") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Icon(Icons.Default.FileOpen, contentDescription = null)
        Text("Import Theme from JSON", modifier = Modifier.padding(start = Spacing.sm))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeRow(
    current: ThemeMode,
    onChanged: (ThemeMode) -> Unit,
) {
    val options = listOf(
        ThemeMode.LIGHT to "Light",
        ThemeMode.DARK to "Dark",
        ThemeMode.SYSTEM to "System",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text("Color Scheme", style = MaterialTheme.typography.bodyLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                    onClick = { onChanged(mode) },
                    selected = current == mode,
                ) {
                    Text(label)
                }
            }
        }
    }
}
