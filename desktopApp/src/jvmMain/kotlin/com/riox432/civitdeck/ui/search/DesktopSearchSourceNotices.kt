package com.riox432.civitdeck.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.feature.search.presentation.SearchSourceNotice
import com.riox432.civitdeck.feature.search.presentation.SearchSourceNoticeReason
import com.riox432.civitdeck.ui.desktopFocusRing
import com.riox432.civitdeck.ui.theme.Spacing

/**
 * Non-blocking provider notices shown above the results: at most one line per kind for each
 * provider, so a provider with several unsupported filters still takes one line for them.
 * Desktop has no pull-to-refresh, so a failed provider gets an explicit Refresh action here.
 */
@Composable
internal fun DesktopSearchSourceNotices(
    notices: List<SearchSourceNotice>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = notices.groupBy { it.source }
        .flatMap { (source, sourceNotices) -> sourceNoticeLines(source, sourceNotices.map { it.reason }.toSet()) }
    val anyFailed = notices.any { it.reason == SearchSourceNoticeReason.SOURCE_FAILED }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                lines.forEach { line ->
                    Text(text = line, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (anyFailed) {
                TextButton(onClick = onRefresh, modifier = Modifier.desktopFocusRing()) {
                    Text("Refresh")
                }
            }
        }
    }
}

private fun sourceNoticeLines(source: ModelSource, reasons: Set<SearchSourceNoticeReason>): List<String> {
    val label = source.displayLabel()
    val unsupported = reasons.mapNotNull { it.unsupportedFilterLabel() }
    return buildList {
        if (SearchSourceNoticeReason.SOURCE_FAILED in reasons) {
            add("Couldn't load $label results.")
        }
        if (unsupported.isNotEmpty()) {
            add("$label ignores: ${unsupported.joinToString(", ")}")
        }
        if (SearchSourceNoticeReason.UNRATED_PREVIEWS_HIDDEN in reasons) {
            add("$label previews are unrated, so they're hidden at your NSFW level.")
        }
    }
}

private fun SearchSourceNoticeReason.unsupportedFilterLabel(): String? = when (this) {
    SearchSourceNoticeReason.UNSUPPORTED_SORT -> "sort order"
    SearchSourceNoticeReason.UNSUPPORTED_PERIOD -> "time period"
    SearchSourceNoticeReason.UNSUPPORTED_BASE_MODEL -> "base model"
    SearchSourceNoticeReason.UNSUPPORTED_FRESH_FIND -> "Fresh Only"
    SearchSourceNoticeReason.UNSUPPORTED_QUALITY_FILTER -> "quality filter"
    SearchSourceNoticeReason.SOURCE_FAILED,
    SearchSourceNoticeReason.UNRATED_PREVIEWS_HIDDEN,
    -> null
}
