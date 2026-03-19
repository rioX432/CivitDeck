@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.BuildConfig
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.model.HiddenModel
import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsUiState
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.StorageSettingsViewModel
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.update.UpdateBanner
import com.riox432.civitdeck.ui.update.UpdateUiState
import com.riox432.civitdeck.ui.update.UpdateViewModel

@Suppress("LongParameterList")
@Composable
fun SettingsScreen(
    authViewModel: AuthSettingsViewModel,
    storageViewModel: StorageSettingsViewModel,
    appBehaviorViewModel: AppBehaviorSettingsViewModel,
    updateViewModel: UpdateViewModel,
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToContentFilter: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToAdvanced: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToNotificationCenter: () -> Unit = {},
    onNavigateToBrowsingHistory: () -> Unit = {},
    onNavigateToLicenses: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    scrollToTopTrigger: Int = 0,
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val storageState by storageViewModel.uiState.collectAsStateWithLifecycle()
    val appBehaviorState by appBehaviorViewModel.uiState.collectAsStateWithLifecycle()
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var lastHandledTrigger by rememberSaveable { mutableIntStateOf(scrollToTopTrigger) }
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger != lastHandledTrigger) {
            lastHandledTrigger = scrollToTopTrigger
            listState.animateScrollToItem(0)
        }
    }
    val isEmpty by remember { derivedStateOf { listState.layoutInfo.totalItemsCount == 0 } }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            if (!storageState.isOnline) {
                item { OfflineBanner() }
            }
            settingsAccountItems(authState, authViewModel)
            item { SectionHeader("Appearance") }
            item { SubScreenRow("Appearance", onNavigateToAppearance) }
            item { SectionHeader("Content & Behavior") }
            item { SubScreenRow("Content & Behavior", onNavigateToContentFilter) }
            item { SectionHeader("Notifications") }
            item { SubScreenRow("Model Updates", onNavigateToNotificationCenter) }
            item { SectionHeader("History") }
            item { SubScreenRow("Browsing History", onNavigateToBrowsingHistory) }
            item { SectionHeader("Data & Storage") }
            item { SubScreenRow("Data & Storage", onNavigateToStorage) }
            item { SectionHeader("Advanced & Integrations") }
            item { SubScreenRow("Advanced & Integrations", onNavigateToAdvanced) }
            if (appBehaviorState.powerUserMode) {
                item { SectionHeader("Analytics") }
                item { SubScreenRow("Usage Stats", onNavigateToAnalytics) }
            }
            settingsUpdateItems(updateState, updateViewModel, onOpenUrl)
            settingsAboutItems(
                appBehaviorState.powerUserMode,
                updateState,
                updateViewModel,
                onNavigateToLicenses,
            )
        }
        if (isEmpty) {
            EmptyStateMessage(
                icon = Icons.Default.Settings,
                title = "No settings available",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun SubScreenRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, onClickLabel = "Open setting")
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = stringResource(R.string.cd_navigate_forward),
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider()
}

internal fun LazyListScope.settingsAccountItems(
    state: AuthSettingsUiState,
    viewModel: AuthSettingsViewModel,
) {
    item { SectionHeader("Account") }
    item {
        AccountSection(
            apiKey = state.apiKey,
            connectedUsername = state.connectedUsername,
            isValidating = state.isValidatingApiKey,
            error = state.apiKeyError,
            onValidateAndSave = viewModel::onValidateAndSaveApiKey,
            onClear = viewModel::onClearApiKey,
        )
    }
}

private fun LazyListScope.settingsUpdateItems(
    updateState: UpdateUiState,
    updateViewModel: UpdateViewModel,
    onOpenUrl: (String) -> Unit,
) {
    if (updateState.showBanner && updateState.updateResult != null) {
        item {
            UpdateBanner(
                updateResult = updateState.updateResult!!,
                onDownload = { onOpenUrl(updateState.updateResult!!.htmlUrl) },
                onDismiss = updateViewModel::dismissBanner,
            )
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.settingsAboutItems(
    powerUserMode: Boolean,
    updateState: UpdateUiState,
    updateViewModel: UpdateViewModel,
    onNavigateToLicenses: () -> Unit,
) {
    item { SectionHeader("About") }
    item { InfoRow("App Version", BuildConfig.VERSION_NAME) }
    item {
        UpdateCheckRow(
            autoCheckEnabled = updateState.autoCheckEnabled,
            isChecking = updateState.isChecking,
            onAutoCheckChanged = updateViewModel::setAutoCheckEnabled,
            onCheckNow = updateViewModel::checkForUpdate,
        )
    }
    item { NavigationRow("Open Source Licenses", onNavigateToLicenses) }
    if (!powerUserMode) {
        item {
            Text(
                "Enable Power User Mode in Advanced & Integrations for more features",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            )
        }
    }
}

@Composable
internal fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}

@Composable
internal fun AccentColorRow(
    selected: AccentColor,
    onChanged: (AccentColor) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Text("Accent Color", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(Spacing.sm))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(AccentColor.entries.toList(), key = { it.name }) { color ->
                AccentColorSwatch(color = color, isSelected = color == selected) {
                    onChanged(color)
                }
            }
        }
    }
}

@Composable
internal fun AccentColorSwatch(
    color: AccentColor,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    // Intentional: seedHex is the user-chosen accent seed color, used here as a literal preview
    // swatch. It cannot be expressed via MaterialTheme tokens because it is the input to the
    // dynamic color pipeline, not a derived token value.
    @Suppress("MagicNumber")
    val swatchColor = Color(color.seedHex)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(swatchColor)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick, onClickLabel = "Select color theme"),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.cd_selected),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun AmoledDarkModeRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("AMOLED Dark Mode", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Use pure black background in dark mode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
internal fun NotificationsToggleRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) onToggle(true) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Model Update Alerts", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Notify when favorited models get new versions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = { newValue ->
                if (newValue && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onToggle(newValue)
                }
            },
        )
    }
}

@Composable
internal fun PollingIntervalRow(selected: PollingInterval, onChanged: (PollingInterval) -> Unit) {
    val options = PollingInterval.entries.filter { it != PollingInterval.Off }
    DropdownSettingRow(
        label = "Check Frequency",
        value = selected.displayName,
        options = options.map { it.displayName },
        onSelected = { name -> options.find { it.displayName == name }?.let(onChanged) },
    )
}

@Composable
internal fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.errorContainer,
                RoundedCornerShape(Spacing.sm),
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "You are offline — showing cached data",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
internal fun OfflineCacheToggleRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Offline Cache", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Keep viewed models available offline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
internal fun CacheSizeLimitRow(
    currentLimitMb: Int,
    currentUsage: String,
    onChanged: (Int) -> Unit,
) {
    val options = listOf(50, 100, 200, 500)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Cache Size Limit", style = MaterialTheme.typography.bodyLarge)
            Text(
                "$currentLimitMb MB (used: $currentUsage)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            options.forEach { mb ->
                TextButton(onClick = { onChanged(mb) }) {
                    Text(
                        text = "${mb}MB",
                        color = if (currentLimitMb == mb) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
internal fun CacheInfoRow(entryCount: Int, formattedSize: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Cached Entries", style = MaterialTheme.typography.bodyLarge)
        Text(
            "$entryCount entries ($formattedSize)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun NsfwToggleRow(level: NsfwFilterLevel, onToggle: (NsfwFilterLevel) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("NSFW Content", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Show NSFW content in search results",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = level != NsfwFilterLevel.Off,
            onCheckedChange = {
                onToggle(if (level == NsfwFilterLevel.Off) NsfwFilterLevel.All else NsfwFilterLevel.Off)
            },
        )
    }
}

@Composable
internal fun NsfwBlurSection(
    settings: NsfwBlurSettings,
    onSettingsChanged: (NsfwBlurSettings) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            "Blur Intensity",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Controls blur strength for NSFW images in the Image Gallery. " +
                "Tap any blurred image to reveal it temporarily.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BlurSliderRow("Soft", settings.softIntensity) {
            onSettingsChanged(settings.copy(softIntensity = it))
        }
        BlurSliderRow("Mature", settings.matureIntensity) {
            onSettingsChanged(settings.copy(matureIntensity = it))
        }
        BlurSliderRow("Explicit", settings.explicitIntensity) {
            onSettingsChanged(settings.copy(explicitIntensity = it))
        }
    }
}

@Composable
internal fun BlurSliderRow(
    label: String,
    intensity: Int,
    onChanged: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (intensity == 0) "Hidden" else "$intensity%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = intensity.toFloat(),
            onValueChange = { onChanged(it.toInt()) },
            valueRange = 0f..100f,
            steps = 3,
        )
    }
}

@Composable
internal fun SortOrderRow(selected: SortOrder, onChanged: (SortOrder) -> Unit) {
    DropdownSettingRow(
        label = "Default Sort",
        value = selected.name,
        options = SortOrder.entries.map { it.name },
        onSelected = { onChanged(SortOrder.valueOf(it)) },
    )
}

@Composable
internal fun TimePeriodRow(selected: TimePeriod, onChanged: (TimePeriod) -> Unit) {
    DropdownSettingRow(
        label = "Default Period",
        value = selected.name,
        options = TimePeriod.entries.map { it.name },
        onSelected = { onChanged(TimePeriod.valueOf(it)) },
    )
}

@Composable
internal fun DropdownSettingRow(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = stringResource(R.string.cd_open_dropdown)) { expanded = true }
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun GridColumnsRow(columns: Int, onChanged: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Grid Columns", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(2, 3).forEach { count ->
                TextButton(onClick = { onChanged(count) }) {
                    Text(
                        text = count.toString(),
                        color = if (columns == count) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PowerUserModeRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Power User Mode", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Enables ComfyUI, SD WebUI, Civitai Link, model files, and advanced metadata",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
internal fun HiddenModelsRow(
    count: Int,
    models: List<HiddenModel>,
    onUnhide: (Long) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    SettingsClickRow(label = "Hidden Models", detail = "$count models") { showDialog = true }
    if (showDialog) {
        HiddenModelsDialog(models = models, onUnhide = onUnhide, onDismiss = { showDialog = false })
    }
}

@Composable
internal fun HiddenModelsDialog(
    models: List<HiddenModel>,
    onUnhide: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hidden Models") },
        text = {
            if (models.isEmpty()) {
                Text("No hidden models.\nLong-press a model card on the Search screen to hide it.")
            } else {
                Column {
                    models.forEach { model ->
                        HiddenModelItem(model.modelName) { onUnhide(model.modelId) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
internal fun HiddenModelItem(name: String, onUnhide: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onUnhide) { Text("Unhide") }
    }
}

@Composable
internal fun ExcludedTagsRow(
    tags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    SettingsClickRow(label = "Excluded Tags", detail = "${tags.size} tags") { showDialog = true }
    if (showDialog) {
        ExcludedTagsDialog(tags = tags, onAdd = onAdd, onRemove = onRemove, onDismiss = { showDialog = false })
    }
}

@Composable
internal fun ExcludedTagsDialog(
    tags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newTag by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluded Tags") },
        text = {
            Column {
                ExcludedTagInput(newTag, onValueChange = { newTag = it }) {
                    onAdd(newTag)
                    newTag = ""
                }
                if (tags.isEmpty()) {
                    Text(
                        "No excluded tags",
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                } else {
                    tags.forEach { tag ->
                        ExcludedTagItem(tag) { onRemove(tag) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
internal fun ExcludedTagInput(value: String, onValueChange: (String) -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Add tag") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAdd, enabled = value.isNotBlank()) { Text("Add") }
    }
}

@Composable
internal fun ExcludedTagItem(tag: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tag, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}

@Composable
internal fun ClearActionRow(label: String, onConfirm: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    SettingsClickRow(label = label) { showDialog = true }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(label) },
            text = { Text("Are you sure? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm()
                    showDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
internal fun SettingsClickRow(label: String, detail: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, onClickLabel = "Open setting")
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun AccountSection(
    apiKey: String?,
    connectedUsername: String?,
    isValidating: Boolean,
    error: String?,
    onValidateAndSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (apiKey != null && connectedUsername != null) {
        ConnectedAccountRow(connectedUsername, onClear)
    } else {
        ApiKeyInputRow(isValidating, error, onValidateAndSave)
    }
}

@Composable
internal fun ConnectedAccountRow(username: String, onClear: () -> Unit) {
    var showConfirmation by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Connected as",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(username, style = MaterialTheme.typography.bodyLarge)
        }
        TextButton(onClick = { showConfirmation = true }) {
            Text("Disconnect", color = MaterialTheme.colorScheme.error)
        }
    }
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Disconnect") },
            text = { Text("Remove your CivitAI API key?") },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    showConfirmation = false
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
internal fun ApiKeyInputRow(
    isValidating: Boolean,
    error: String?,
    onValidateAndSave: (String) -> Unit,
) {
    var keyInput by rememberSaveable { mutableStateOf("") }
    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                placeholder = { Text("Paste API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = error != null,
                modifier = Modifier.weight(1f),
            )
            if (isValidating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                TextButton(
                    onClick = {
                        onValidateAndSave(keyInput)
                        keyInput = ""
                    },
                    enabled = keyInput.isNotBlank(),
                ) { Text("Verify") }
            }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Text(
            "Get your key at civitai.com/user/account",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

@Composable
internal fun FeedQualityThresholdRow(threshold: Int, onChanged: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Quality Threshold", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (threshold == 0) "Off" else "$threshold",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "Filter low-quality models from your creator feed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = threshold.toFloat(),
            onValueChange = { onChanged(it.toInt()) },
            valueRange = 0f..100f,
        )
    }
}

@Composable
internal fun UpdateCheckRow(
    autoCheckEnabled: Boolean,
    isChecking: Boolean,
    onAutoCheckChanged: (Boolean) -> Unit,
    onCheckNow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-check for updates", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Check for new versions on launch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = autoCheckEnabled, onCheckedChange = onAutoCheckChanged)
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            TextButton(onClick = onCheckNow, enabled = !isChecking) {
                Text(if (isChecking) "Checking..." else "Check now")
            }
            if (isChecking) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
internal fun NavigationRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, onClickLabel = "Open setting")
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider()
}
