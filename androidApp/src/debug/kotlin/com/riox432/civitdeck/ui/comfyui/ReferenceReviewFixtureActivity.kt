package com.riox432.civitdeck.ui.comfyui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.ComfyUIJobTarget
import com.riox432.civitdeck.domain.model.ComparisonKind
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.ReferenceResourceReview
import com.riox432.civitdeck.domain.model.ReferenceSetting
import com.riox432.civitdeck.domain.model.ResourceConfidence
import com.riox432.civitdeck.domain.model.ResourceReview
import com.riox432.civitdeck.domain.model.SettingComparison
import com.riox432.civitdeck.domain.model.SourceGenerationSnapshot
import com.riox432.civitdeck.domain.model.SourceProvider
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.domain.model.TargetCapability
import com.riox432.civitdeck.feature.comfyui.presentation.ReferenceReviewState
import com.riox432.civitdeck.feature.comfyui.presentation.ReferenceTrialStatus
import com.riox432.civitdeck.ui.theme.CivitDeckTheme

/**
 * Debug-only screen that renders the reference review UI from fixed states, with no view model and
 * no network, and lists every callback it receives so a capture shows what a tap would have done.
 *
 * `adb shell am start -n com.riox432.civitdeck.debug/com.riox432.civitdeck.ui.comfyui.ReferenceReviewFixtureActivity
 * --es scenario MISSING_RESOURCE`
 */
class ReferenceReviewFixtureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initial = intent.getStringExtra(EXTRA_SCENARIO)
            ?.let { name -> ReviewFixture.entries.firstOrNull { it.name == name } }
            ?: ReviewFixture.COMPLETE_FILENAME_ONLY
        setContent {
            CivitDeckTheme {
                Surface(modifier = Modifier.fillMaxSize()) { FixtureScreen(initial) }
            }
        }
    }

    private companion object {
        const val EXTRA_SCENARIO = "scenario"
    }
}

internal enum class ReviewFixture { COMPLETE_FILENAME_ONLY, METADATA_FREE, MISSING_RESOURCE, STALE }

@Composable
private fun FixtureScreen(initial: ReviewFixture) {
    var fixture by remember { mutableStateOf(initial) }
    var state by remember { mutableStateOf(fixtureState(initial)) }
    val calls = remember { mutableStateListOf<String>() }
    val actions = remember {
        ReferenceReviewActions(
            onReview = { calls += "review" },
            onAcknowledge = { checked ->
                calls += "acknowledge($checked)"
                state = state.copy(variationAcknowledged = checked)
            },
            // Mirrors the controller: Confirm only acts on a confirmable review, then leaves it.
            onConfirm = {
                calls += "confirm"
                if (state.canConfirm) state = state.copy(status = ReferenceTrialStatus.SUBMITTED)
            },
            onDismiss = { calls += "dismiss" },
            onUnlink = { calls += "unlink" },
            onInterrupt = { calls += "interrupt" },
        )
    }
    Column(
        modifier = Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FixturePicker(fixture) {
            fixture = it
            state = fixtureState(it)
            calls.clear()
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            ReferenceActionRow(state, GenerationStatus.Idle, actions)
        }
        HorizontalDivider()
        ReferenceReviewContent(state, actions)
        HorizontalDivider()
        Text(
            text = "Callbacks: ${calls.joinToString().ifEmpty { "none" }}\n" +
                "canConfirm=${state.canConfirm} status=${state.status} network=none",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun FixturePicker(selected: ReviewFixture, onSelect: (ReviewFixture) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReviewFixture.entries.forEach { fixture ->
            FilterChip(
                selected = fixture == selected,
                onClick = { onSelect(fixture) },
                label = { Text(fixture.name) },
            )
        }
    }
}

private val TARGET = ComfyUIJobTarget(
    connectionId = 1,
    hostname = "192.168.1.20",
    port = 8188,
    useHttps = false,
    acceptSelfSigned = false,
    tlsCertSha256 = null,
)

private val RECORDED = SourceReference(
    provider = SourceProvider.Civitai,
    artworkId = "41827733",
    artworkUrl = "https://civitai.com/images/41827733",
    creator = "fixture_creator",
    generation = SourceGenerationSnapshot(
        prompt = "a lighthouse at dusk, oil painting",
        sampler = "DPM++ 2M",
        cfgScale = 6.0,
        steps = 28,
        seed = 1234567,
        model = "dreamshaper_8",
        size = "832x1216",
    ),
)

private val METADATA_FREE_SOURCE = RECORDED.copy(
    artworkId = "41827734",
    artworkUrl = "https://civitai.com/images/41827734",
    generation = null,
)

internal fun fixtureState(fixture: ReviewFixture): ReferenceReviewState = when (fixture) {
    ReviewFixture.COMPLETE_FILENAME_ONLY -> reviewed(RECORDED, recordedSettings(), completeResources())
    ReviewFixture.METADATA_FREE -> reviewed(METADATA_FREE_SOURCE, metadataFreeSettings(), emptyList())
    ReviewFixture.MISSING_RESOURCE -> reviewed(RECORDED, recordedSettings(), missingResources())
    // An input changed after review: the payload and its review are gone, so nothing can be confirmed.
    ReviewFixture.STALE -> ReferenceReviewState(
        referenceId = 1,
        source = RECORDED,
        isLoadingSource = false,
        isReviewVisible = true,
        status = ReferenceTrialStatus.DRAFT,
    )
}

private fun reviewed(
    source: SourceReference,
    settings: List<SettingComparison>,
    resources: List<ResourceReview>,
) = ReferenceReviewState(
    referenceId = 1,
    source = source,
    isLoadingSource = false,
    isReviewVisible = true,
    status = ReferenceTrialStatus.REVIEWED,
    target = TARGET,
    review = ReferenceResourceReview(TARGET, TargetCapability.Supported, resources, settings),
)

private fun recordedSettings() = listOf(
    SettingComparison(ReferenceSetting.PROMPT, RECORDED_PROMPT, RECORDED_PROMPT, ComparisonKind.MATCH),
    SettingComparison(ReferenceSetting.MODEL, "dreamshaper_8", "dreamshaper_8", ComparisonKind.MATCH),
    SettingComparison(ReferenceSetting.DIMENSIONS, "832x1216", "832x1216", ComparisonKind.MATCH),
    SettingComparison(ReferenceSetting.STEPS, "28", "28", ComparisonKind.MATCH),
    SettingComparison(ReferenceSetting.CFG, "6.0", "6.0", ComparisonKind.MATCH),
    SettingComparison(ReferenceSetting.SAMPLER, "dpm++_2m", "dpmpp_2m", ComparisonKind.VARIATION),
    SettingComparison(ReferenceSetting.SCHEDULER, null, "karras", ComparisonKind.SOURCE_UNAVAILABLE),
    SettingComparison(ReferenceSetting.SEED, "1234567", "1234567", ComparisonKind.MATCH),
    SettingComparison(
        ReferenceSetting.LORA_WEIGHT,
        "0.8",
        "0.8",
        ComparisonKind.MATCH,
        subject = "add_detail.safetensors",
    ),
)

private fun metadataFreeSettings() = listOf(
    SettingComparison(ReferenceSetting.PROMPT, null, "a lighthouse", ComparisonKind.SOURCE_UNAVAILABLE),
    SettingComparison(ReferenceSetting.MODEL, null, "dreamshaper_8", ComparisonKind.SOURCE_UNAVAILABLE),
    SettingComparison(ReferenceSetting.DIMENSIONS, null, "1024x1024", ComparisonKind.SOURCE_UNAVAILABLE),
    SettingComparison(ReferenceSetting.STEPS, null, "20", ComparisonKind.SOURCE_UNAVAILABLE),
    SettingComparison(ReferenceSetting.SEED, null, "880122", ComparisonKind.SOURCE_UNAVAILABLE),
)

private fun completeResources() = listOf(
    ResourceReview(
        versionId = 128713,
        type = "Checkpoint",
        confidence = ResourceConfidence.FILENAME_ONLY,
        evidence = "Same file name; no hash is known for this server's file",
        targetFiles = listOf("dreamshaper_8.safetensors"),
        selected = true,
        ownedLocally = true,
    ),
    ResourceReview(
        versionId = 62833,
        type = "LORA",
        confidence = ResourceConfidence.VERIFIED,
        evidence = "SHA-256 matches",
        targetFiles = listOf("add_detail.safetensors"),
        selected = true,
    ),
)

private fun missingResources() = completeResources().take(1) + ResourceReview(
    versionId = 62833,
    type = "LORA",
    confidence = ResourceConfidence.MISSING,
    evidence = "SHA-256 of add_detail.safetensors differs",
    targetFiles = listOf("add_detail.safetensors"),
    selected = true,
)

private const val RECORDED_PROMPT = "a lighthouse at dusk, oil painting"
