package com.riox432.civitdeck.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.ui.components.SwipeableModelCard

@Composable
internal fun ModelGridItem(
    model: Model,
    isFavorite: Boolean,
    thumbnailUrl: String?,
    isOwned: Boolean,
    isComparing: Boolean,
    callbacks: ModelGridCallbacks,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        SwipeableModelCard(
            model = model,
            isFavorite = isFavorite,
            onFavoriteToggle = { callbacks.onToggleFavorite(model) },
            onHide = { callbacks.onHideModel(model.id, model.name) },
            onClick = { callbacks.onModelClick(model.id, thumbnailUrl, "") },
            onLongPress = { showMenu = true },
            isOwned = isOwned,
        )
        ModelContextMenu(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            showCompare = !isComparing,
            onCompare = { callbacks.onCompareModel(model.id, model.name) },
            onHide = { callbacks.onHideModel(model.id, model.name) },
        )
    }
}

@Composable
internal fun ModelContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    showCompare: Boolean,
    onCompare: () -> Unit,
    onHide: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (showCompare) {
            DropdownMenuItem(
                text = { Text("Compare") },
                onClick = {
                    onCompare()
                    onDismiss()
                },
                leadingIcon = {
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.cd_compare))
                },
            )
        }
        DropdownMenuItem(
            text = { Text("Hide model") },
            onClick = {
                onHide()
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.VisibilityOff, contentDescription = stringResource(R.string.cd_hide_model))
            },
        )
    }
}
