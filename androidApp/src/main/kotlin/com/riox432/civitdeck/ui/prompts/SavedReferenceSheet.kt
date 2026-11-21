package com.riox432.civitdeck.ui.prompts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.SourceGenerationSnapshot
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.domain.model.SourceResource
import com.riox432.civitdeck.feature.prompts.presentation.ResourceVersionState
import com.riox432.civitdeck.feature.prompts.presentation.SavedReferenceDetailState
import com.riox432.civitdeck.feature.prompts.presentation.SavedReferenceViewModel
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

/**
 * Shows the snapshot saved for row [referenceId] as it was recorded. Resource versions are looked
 * up only by their recorded ID, and links open only when the user taps them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedReferenceSheet(
    viewModel: SavedReferenceViewModel,
    referenceId: Long,
    onDismiss: () -> Unit,
    footer: @Composable () -> Unit = {},
) {
    LaunchedEffect(referenceId) { viewModel.loadReference(referenceId) }
    val detail by viewModel.detailState.collectAsStateWithLifecycle()
    val resources by viewModel.resources.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(stringResource(R.string.saved_reference_title), style = MaterialTheme.typography.titleMedium)
            when (val state = detail) {
                // The previous row's state can still be showing until loadReference runs.
                is SavedReferenceDetailState.Loaded ->
                    if (state.prompt.id == referenceId) {
                        LoadedReference(state, resources, viewModel::inspectResource, uriHandler::openUri)
                    } else {
                        LoadingRow()
                    }
                SavedReferenceDetailState.NotFound -> Message(stringResource(R.string.saved_reference_not_found))
                SavedReferenceDetailState.Failed -> Message(stringResource(R.string.saved_reference_load_failed))
                SavedReferenceDetailState.Idle, SavedReferenceDetailState.Loading -> LoadingRow()
            }
            footer()
        }
    }
}

@Composable
private fun LoadedReference(
    detail: SavedReferenceDetailState.Loaded,
    resources: Map<Long, ResourceVersionState>,
    onInspect: (Long) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val reference = detail.reference
    if (reference == null) {
        val message = if (detail.sourceUnavailable) {
            R.string.saved_reference_snapshot_unreadable
        } else {
            R.string.saved_reference_not_found
        }
        Message(stringResource(message))
        return
    }
    SourceSection(reference, onOpenLink)
    GenerationSection(reference.generation)
    ResourcesSection(reference.resources, resources, onInspect, onOpenLink)
    UsageLinksSection(reference.usageLinks, onOpenLink)
}

@Composable
private fun SourceSection(reference: SourceReference, onOpenLink: (String) -> Unit) {
    SectionTitle(stringResource(R.string.saved_reference_source_section))
    LabeledValue(stringResource(R.string.saved_reference_artwork), reference.artworkId)
    LabeledValue(stringResource(R.string.saved_reference_creator), reference.creator)
    LabeledValue(stringResource(R.string.saved_reference_content_rating), reference.contentRating)
    TextButton(onClick = { onOpenLink(reference.artworkUrl) }) {
        Text(stringResource(R.string.saved_reference_open_artwork))
    }
}

@Composable
private fun GenerationSection(generation: SourceGenerationSnapshot?) {
    SectionTitle(stringResource(R.string.saved_reference_generation_section))
    if (generation == null) {
        Message(stringResource(R.string.saved_reference_no_generation))
        return
    }
    LabeledValue(stringResource(R.string.saved_reference_prompt), generation.prompt)
    LabeledValue(stringResource(R.string.saved_reference_negative_prompt), generation.negativePrompt)
    LabeledValue(stringResource(R.string.saved_reference_sampler), generation.sampler)
    LabeledValue(stringResource(R.string.saved_reference_steps), generation.steps?.toString())
    LabeledValue(stringResource(R.string.saved_reference_cfg_scale), generation.cfgScale?.toString())
    LabeledValue(stringResource(R.string.saved_reference_seed), generation.seed?.toString())
    LabeledValue(stringResource(R.string.saved_reference_model), generation.model)
    LabeledValue(stringResource(R.string.saved_reference_size), generation.size)
}

@Composable
private fun ResourcesSection(
    recorded: List<SourceResource>,
    inspected: Map<Long, ResourceVersionState>,
    onInspect: (Long) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    SectionTitle(stringResource(R.string.saved_reference_resources_section))
    if (recorded.isEmpty()) {
        Message(stringResource(R.string.saved_reference_no_resources))
        return
    }
    recorded.forEach { resource ->
        ResourceCard(resource, inspected[resource.versionId], onInspect, onOpenLink)
    }
}

@Composable
private fun ResourceCard(
    resource: SourceResource,
    state: ResourceVersionState?,
    onInspect: (Long) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CornerRadius.card)) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                stringResource(R.string.saved_reference_version, resource.versionId.toString()),
                style = MaterialTheme.typography.titleSmall,
            )
            LabeledValue(stringResource(R.string.saved_reference_type), resource.type)
            // Only a weight the source stated; no default is filled in.
            LabeledValue(stringResource(R.string.saved_reference_weight), resource.weight?.toString())
            ResourceVersionDetails(resource.versionId, state, onInspect, onOpenLink)
        }
    }
}

@Composable
private fun ResourceVersionDetails(
    versionId: Long,
    state: ResourceVersionState?,
    onInspect: (Long) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    when (state) {
        null -> OutlinedButton(onClick = { onInspect(versionId) }) {
            Text(stringResource(R.string.saved_reference_inspect_version))
        }
        is ResourceVersionState.Loading -> CircularProgressIndicator(modifier = Modifier.size(PROGRESS_SIZE))
        is ResourceVersionState.Available -> {
            Text(state.name, style = MaterialTheme.typography.bodyMedium)
            LabeledValue(stringResource(R.string.saved_reference_base_model), state.baseModel)
            LabeledValue(
                stringResource(R.string.saved_reference_files),
                state.files.joinToString("\n") { it.name }.ifEmpty { null },
            )
            TextButton(onClick = { onOpenLink(state.url) }) {
                Text(stringResource(R.string.saved_reference_open_version, versionId.toString()))
            }
        }
        is ResourceVersionState.Unavailable -> {
            Message(stringResource(R.string.saved_reference_version_unavailable, versionId.toString()))
            TextButton(onClick = { onInspect(versionId) }) {
                Text(stringResource(R.string.saved_reference_retry_version))
            }
        }
    }
}

@Composable
private fun UsageLinksSection(links: List<String>, onOpenLink: (String) -> Unit) {
    if (links.isEmpty()) return
    SectionTitle(stringResource(R.string.saved_reference_usage_section))
    links.forEach { link ->
        // Stored links are shown as recorded; only HTTPS ones are offered as tappable links.
        if (link.startsWith(HTTPS_PREFIX)) {
            TextButton(onClick = { onOpenLink(link) }) { Text(link) }
        } else {
            Message(link)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun LabeledValue(label: String, value: String?) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val recorded = value?.takeIf { it.isNotBlank() }
        Text(
            text = recorded ?: stringResource(R.string.saved_reference_not_recorded),
            style = MaterialTheme.typography.bodySmall,
            color = if (recorded == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun Message(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LoadingRow() {
    Box(modifier = Modifier.fillMaxWidth().padding(Spacing.lg), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private const val HTTPS_PREFIX = "https://"
private val PROGRESS_SIZE = 24.dp
