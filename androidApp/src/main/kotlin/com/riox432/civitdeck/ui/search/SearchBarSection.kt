package com.riox432.civitdeck.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.zIndex
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import com.riox432.civitdeck.ui.theme.Spacing
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

internal class CollapsibleHeaderState {
    var headerHeightPx by mutableFloatStateOf(0f)
    var offsetPx by mutableFloatStateOf(0f)
    val animatable = Animatable(0f)

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (headerHeightPx <= 0f) return Offset.Zero
            val newOffset = (offsetPx + available.y).coerceIn(-headerHeightPx, 0f)
            val consumed = newOffset - offsetPx
            offsetPx = newOffset
            return Offset(0f, consumed)
        }
    }
}

@Composable
internal fun rememberCollapsibleHeaderState(): CollapsibleHeaderState {
    return remember { CollapsibleHeaderState() }
}

@Composable
internal fun HeaderSnapEffect(gridState: LazyGridState, headerState: CollapsibleHeaderState) {
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .drop(1)
            .collectLatest { scrolling ->
                if (!scrolling && headerState.headerHeightPx > 0f) {
                    val threshold = -headerState.headerHeightPx / 2f
                    val target = if (headerState.offsetPx < threshold) {
                        -headerState.headerHeightPx
                    } else {
                        0f
                    }
                    headerState.animatable.snapTo(headerState.offsetPx)
                    headerState.animatable.animateTo(
                        targetValue = target,
                        animationSpec = tween(
                            durationMillis = Duration.fast,
                            easing = Easing.standard,
                        ),
                    ) {
                        headerState.offsetPx = value
                    }
                }
            }
    }
}

@Composable
internal fun CollapsibleHeader(
    headerState: CollapsibleHeaderState,
    query: String,
    searchHistory: List<String>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onHistoryItemClick: (String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .graphicsLayer { translationY = headerState.offsetPx }
            .onGloballyPositioned { coordinates ->
                headerState.headerHeightPx = coordinates.size.height.toFloat()
            }
            .background(MaterialTheme.colorScheme.surface),
    ) {
        SearchBarWithFilterButton(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            searchHistory = searchHistory,
            onHistoryItemClick = onHistoryItemClick,
            onDeleteHistoryItem = onDeleteHistoryItem,
            onClearHistory = onClearHistory,
        )
    }
}

@Composable
private fun SearchBarWithFilterButton(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    searchHistory: List<String>,
    onHistoryItemClick: (String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var showHistory by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        SearchTextField(
            query = query,
            onQueryChange = {
                onQueryChange(it)
                showHistory = it.isEmpty() && searchHistory.isNotEmpty()
            },
            onSearch = {
                onSearch()
                keyboardController?.hide()
                showHistory = false
            },
            onFocusChanged = { focused ->
                if (!focused) onSearch()
                showHistory = focused && query.isEmpty() && searchHistory.isNotEmpty()
            },
            onClear = {
                onQueryChange("")
                onSearch()
                showHistory = searchHistory.isNotEmpty()
            },
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(visible = showHistory) {
            SearchHistoryDropdown(
                history = searchHistory,
                onItemClick = { item ->
                    onHistoryItemClick(item)
                    showHistory = false
                    keyboardController?.hide()
                },
                onDeleteItem = { item -> onDeleteHistoryItem(item) },
                onClearAll = {
                    onClearHistory()
                    showHistory = false
                },
            )
        }
    }
}

@Composable
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.onFocusChanged { onFocusChanged(it.isFocused) },
        placeholder = { Text("Search models...") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    )
}

@Composable
private fun SearchHistoryDropdown(
    history: List<String>,
    onItemClick: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(bottomStart = CornerRadius.card, bottomEnd = CornerRadius.card),
            )
            .padding(vertical = Spacing.xs),
    ) {
        history.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = { onItemClick(item) },
                        onClickLabel = "Select search suggestion",
                    )
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onDeleteItem(item) }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = "Clear history",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = onClearAll, onClickLabel = "Clear all filters")
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )
    }
}
