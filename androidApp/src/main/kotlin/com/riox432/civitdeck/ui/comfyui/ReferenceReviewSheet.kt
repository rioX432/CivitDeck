package com.riox432.civitdeck.ui.comfyui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.CapabilityProblem
import com.riox432.civitdeck.domain.model.ComparisonKind
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.ReferenceSetting
import com.riox432.civitdeck.domain.model.ResourceConfidence
import com.riox432.civitdeck.domain.model.ResourceReview
import com.riox432.civitdeck.domain.model.SettingComparison
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.domain.model.TargetCapability
import com.riox432.civitdeck.feature.comfyui.presentation.ReferenceReviewState
import com.riox432.civitdeck.feature.comfyui.presentation.ReferenceTrialProblem
import com.riox432.civitdeck.feature.comfyui.presentation.ReferenceTrialStatus
import com.riox432.civitdeck.ui.theme.Spacing

/** Callbacks of a linked reference; each maps to one shared view model call. */
internal class ReferenceReviewActions(
    val onReview: () -> Unit,
    val onAcknowledge: (Boolean) -> Unit,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
    val onUnlink: () -> Unit,
    val onInterrupt: () -> Unit,
)

internal fun ReferenceReviewState.isPreparing(): Boolean =
    isLoadingSource || status == ReferenceTrialStatus.PREPARING

// Review only prepares a payload and never submits, so it stays available whenever the form
// is idle; an incomplete draft is reported by the shared state instead of a disabled button.
internal fun canStartReview(review: ReferenceReviewState, generationStatus: GenerationStatus): Boolean =
    !review.isPreparing() &&
        review.source != null &&
        generationStatus != GenerationStatus.Submitting &&
        generationStatus != GenerationStatus.Running

/** Replaces the Generate row while a reference is linked. */
@Composable
internal fun ReferenceActionRow(
    review: ReferenceReviewState,
    generationStatus: GenerationStatus,
    actions: ReferenceReviewActions,
) {
    val isGenerating = generationStatus == GenerationStatus.Submitting ||
        generationStatus == GenerationStatus.Running
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(stringResource(R.string.reference_linked_title), style = MaterialTheme.typography.titleSmall)
            review.source?.let { ArtworkIdentity(it) }
            if (review.isLoadingSource) BodyNote(stringResource(R.string.reference_loading_source))
            BodyNote(stringResource(R.string.reference_linked_manual_tools))
            review.problem?.let { ProblemText(it, review.errorDetail) }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(
                    onClick = actions.onReview,
                    enabled = canStartReview(review, generationStatus),
                    modifier = Modifier.weight(1f),
                ) {
                    if (review.isPreparing() || isGenerating) SmallProgress()
                    Text(
                        stringResource(
                            if (isGenerating) R.string.label_generating else R.string.reference_action_review
                        ),
                    )
                }
                if (isGenerating) StopButton(actions.onInterrupt)
            }
            TextButton(onClick = actions.onUnlink) { Text(stringResource(R.string.reference_action_unlink)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReferenceReviewSheet(review: ReferenceReviewState, actions: ReferenceReviewActions) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = actions.onDismiss, sheetState = sheetState) {
        ReferenceReviewContent(
            review = review,
            actions = actions,
            modifier = Modifier.verticalScroll(rememberScrollState()),
        )
    }
}

/** The sheet's body, shown from shared state only: nothing is rebuilt or inferred here. */
@Composable
internal fun ReferenceReviewContent(
    review: ReferenceReviewState,
    actions: ReferenceReviewActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(stringResource(R.string.reference_review_title), style = MaterialTheme.typography.titleLarge)
        review.source?.let { Section(R.string.reference_section_artwork) { ArtworkIdentity(it) } }
        when {
            review.status == ReferenceTrialStatus.PREPARING -> Row(verticalAlignment = Alignment.CenterVertically) {
                SmallProgress()
                BodyNote(stringResource(R.string.reference_review_preparing))
            }
            review.status != ReferenceTrialStatus.REVIEWED || review.review == null ->
                BodyNote(stringResource(R.string.reference_review_not_current))
            else -> ReviewedDetails(review, actions.onAcknowledge)
        }
        review.problem?.let { ProblemText(it, review.errorDetail) }
        ReviewActions(review, actions)
    }
}

@Composable
private fun ReviewedDetails(state: ReferenceReviewState, onAcknowledge: (Boolean) -> Unit) {
    val review = state.review ?: return
    Section(R.string.reference_section_target) {
        Text(review.target.baseUrl, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
    CapabilityNote(review.capability)
    Section(R.string.reference_section_settings) {
        review.settings.forEach { SettingRow(it) }
    }
    if (review.resources.isNotEmpty()) {
        Section(R.string.reference_section_resources) {
            review.resources.forEach { ResourceRow(it) }
        }
    }
    if (review.resources.any { it.selected && it.confidence == ResourceConfidence.MISSING }) {
        Text(
            stringResource(R.string.reference_missing_resource_blocks),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    if (review.requiresVariationAcknowledgement) {
        VariationAcknowledgement(state.variationAcknowledged, onAcknowledge)
    }
}

@Composable
private fun VariationAcknowledgement(acknowledged: Boolean, onAcknowledge: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = acknowledged, role = Role.Checkbox, onValueChange = onAcknowledge),
    ) {
        Checkbox(checked = acknowledged, onCheckedChange = null)
        Text(
            stringResource(R.string.reference_variation_acknowledgement),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = Spacing.sm),
        )
    }
}

@Composable
private fun ReviewActions(review: ReferenceReviewState, actions: ReferenceReviewActions) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Button(
            onClick = actions.onConfirm,
            enabled = review.canConfirm,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.reference_action_confirm)) }
        OutlinedButton(onClick = actions.onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reference_action_return))
        }
        TextButton(onClick = actions.onUnlink, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reference_action_unlink))
        }
    }
}

@Composable
private fun Section(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(stringResource(title), style = MaterialTheme.typography.labelLarge)
        content()
        HorizontalDivider(modifier = Modifier.padding(top = Spacing.xs))
    }
}

@Composable
private fun ArtworkIdentity(source: SourceReference) {
    Column {
        Text(
            stringResource(R.string.reference_artwork_identity, source.artworkId),
            style = MaterialTheme.typography.bodyMedium,
        )
        source.creator?.let {
            BodyNote(stringResource(R.string.reference_artwork_creator, it))
        }
        BodyNote(source.artworkUrl)
    }
}

@Composable
private fun CapabilityNote(capability: TargetCapability) {
    when (capability) {
        TargetCapability.Supported -> Unit
        is TargetCapability.Unavailable -> ErrorNote(
            stringResource(R.string.reference_capability_unavailable, capability.reason),
        )
        is TargetCapability.Blocked -> Column {
            ErrorNote(stringResource(R.string.reference_capability_blocked))
            capability.problems.forEach { ErrorNote(capabilityProblemText(it)) }
        }
    }
}

@Composable
private fun capabilityProblemText(problem: CapabilityProblem): String = when (problem) {
    is CapabilityProblem.MissingNode -> stringResource(R.string.reference_capability_missing_node, problem.classType)
    is CapabilityProblem.UnsupportedChoice -> stringResource(
        R.string.reference_capability_unsupported_choice,
        problem.classType,
        problem.input,
        problem.value,
    )
    is CapabilityProblem.UnknownInput ->
        stringResource(R.string.reference_capability_unknown_input, problem.classType, problem.input)
}

@Composable
private fun SettingRow(comparison: SettingComparison) {
    Column(modifier = Modifier.padding(vertical = Spacing.xxs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val name = stringResource(settingLabel(comparison.setting))
            Text(
                comparison.subject?.let { "$name · $it" } ?: name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(comparisonLabel(comparison.kind)),
                style = MaterialTheme.typography.labelSmall,
                color = comparisonColor(comparison.kind),
            )
        }
        ValueLine(
            R.string.reference_source_value,
            comparison.source ?: stringResource(R.string.reference_value_not_recorded),
        )
        ValueLine(
            R.string.reference_effective_value,
            comparison.effective ?: stringResource(R.string.reference_value_not_used),
        )
    }
}

@Composable
private fun ValueLine(@StringRes format: Int, value: String) {
    Text(
        stringResource(format, value),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ResourceRow(resource: ResourceReview) {
    Column(modifier = Modifier.padding(vertical = Spacing.xxs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    R.string.reference_resource_version,
                    resource.type ?: stringResource(R.string.reference_resource_unknown_type),
                    resource.versionId,
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(confidenceLabel(resource.confidence)),
                style = MaterialTheme.typography.labelSmall,
                color = confidenceColor(resource.confidence),
            )
        }
        if (resource.selected && resource.targetFiles.isNotEmpty()) {
            BodyNote(stringResource(R.string.reference_resource_selected, resource.targetFiles.joinToString()))
        }
        if (resource.ownedLocally) BodyNote(stringResource(R.string.reference_resource_owned_locally))
    }
}

@StringRes
internal fun confidenceLabel(confidence: ResourceConfidence): Int = when (confidence) {
    ResourceConfidence.VERIFIED -> R.string.reference_resource_verified
    ResourceConfidence.FILENAME_ONLY -> R.string.reference_resource_filename_only
    ResourceConfidence.MISSING -> R.string.reference_resource_missing
    ResourceConfidence.UNKNOWN -> R.string.reference_resource_unknown
}

@StringRes
internal fun comparisonLabel(kind: ComparisonKind): Int = when (kind) {
    ComparisonKind.MATCH -> R.string.reference_comparison_match
    ComparisonKind.VARIATION -> R.string.reference_comparison_variation
    ComparisonKind.SOURCE_UNAVAILABLE -> R.string.reference_comparison_unavailable
}

@StringRes
private fun settingLabel(setting: ReferenceSetting): Int = when (setting) {
    ReferenceSetting.PROMPT -> R.string.reference_setting_prompt
    ReferenceSetting.MODEL -> R.string.reference_setting_model
    ReferenceSetting.DIMENSIONS -> R.string.reference_setting_dimensions
    ReferenceSetting.STEPS -> R.string.reference_setting_steps
    ReferenceSetting.CFG -> R.string.reference_setting_cfg
    ReferenceSetting.SAMPLER -> R.string.reference_setting_sampler
    ReferenceSetting.SCHEDULER -> R.string.reference_setting_scheduler
    ReferenceSetting.SEED -> R.string.reference_setting_seed
    ReferenceSetting.LORA_WEIGHT -> R.string.reference_setting_lora_weight
}

@Composable
private fun confidenceColor(confidence: ResourceConfidence): Color = when (confidence) {
    ResourceConfidence.VERIFIED -> MaterialTheme.colorScheme.primary
    ResourceConfidence.FILENAME_ONLY -> MaterialTheme.colorScheme.tertiary
    ResourceConfidence.MISSING -> MaterialTheme.colorScheme.error
    ResourceConfidence.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun comparisonColor(kind: ComparisonKind): Color = when (kind) {
    ComparisonKind.MATCH -> MaterialTheme.colorScheme.primary
    ComparisonKind.VARIATION -> MaterialTheme.colorScheme.tertiary
    ComparisonKind.SOURCE_UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun ProblemText(problem: ReferenceTrialProblem, detail: String?) {
    val text = when (problem) {
        ReferenceTrialProblem.SOURCE_UNAVAILABLE -> stringResource(R.string.reference_problem_source_unavailable)
        ReferenceTrialProblem.INCOMPLETE_DRAFT -> stringResource(R.string.reference_problem_incomplete_draft)
        ReferenceTrialProblem.NO_ACTIVE_CONNECTION -> stringResource(R.string.reference_problem_no_connection)
        ReferenceTrialProblem.PREPARATION_FAILED ->
            stringResource(R.string.reference_problem_preparation_failed, detail.orEmpty())
    }
    ErrorNote(text)
}

@Composable
private fun BodyNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ErrorNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun SmallProgress() {
    CircularProgressIndicator(modifier = Modifier.padding(end = Spacing.sm).size(18.dp), strokeWidth = 2.dp)
}

@Composable
private fun StopButton(onInterrupt: () -> Unit) {
    Button(
        onClick = onInterrupt,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
    ) {
        Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.cd_stop_generation))
    }
}
