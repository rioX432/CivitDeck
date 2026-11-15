package com.riox432.civitdeck.ui.search

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.ui.components.ModelCard
import com.riox432.civitdeck.ui.components.SwipeableModelCard
import com.riox432.civitdeck.ui.testing.DiscoveryTestTags

@Composable
internal fun ModelGridItem(
    model: Model,
    isFavorite: Boolean,
    thumbnailUrl: String?,
    isOwned: Boolean,
    isComparing: Boolean,
    callbacks: ModelGridCallbacks,
    externalSharedElementSuffix: String,
    modifier: Modifier = Modifier,
) {
    if (!model.nativeActionsSupported) {
        ExternalModelGridItem(
            model = model,
            sharedElementSuffix = externalSharedElementSuffix,
            modifier = modifier,
        )
        return
    }
    var showMenu by remember { mutableStateOf(false) }
    Box(modifier = modifier.testTag(DiscoveryTestTags.MODEL_CARD)) {
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

/**
 * Card for a model from a provider other than CivitAI. Its numeric ID is synthetic, so it gets no
 * favorite/hide/compare action and opens the provider's own page instead of native detail.
 */
@Composable
private fun ExternalModelGridItem(
    model: Model,
    sharedElementSuffix: String,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val unavailableMessage = stringResource(R.string.search_external_link_unavailable)
    val url = model.reference.canonicalUrl
    Box(modifier = modifier.testTag(DiscoveryTestTags.MODEL_CARD)) {
        ModelCard(
            model = model,
            onClick = {
                // openUri throws when no installed app can handle the link.
                val opened = url != null && runCatching { uriHandler.openUri(url) }.isSuccess
                if (!opened) Toast.makeText(context, unavailableMessage, Toast.LENGTH_SHORT).show()
            },
            sharedElementSuffix = sharedElementSuffix,
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
                text = { Text(stringResource(R.string.search_compare)) },
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
            text = { Text(stringResource(R.string.search_hide_model)) },
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
