package com.riox432.civitdeck.ui.gallery

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.feature.prompts.presentation.SaveReferenceState
import com.riox432.civitdeck.feature.prompts.presentation.SavedReferenceViewModel
import com.riox432.civitdeck.ui.theme.Spacing

/** Lets the viewer save the current artwork's source reference; null hides the action. */
data class ReferenceSaving(
    val state: SaveReferenceState,
    val onSave: (Image) -> Unit,
)

internal enum class ReferenceSaveButton { Hidden, Ready, Busy, Saving, Saved, Failed }

/**
 * The save action for the page showing [image]. [SaveReferenceState] tracks only the latest
 * request, so the page that submitted it ([submittedArtworkId]) and the artworks saved earlier in
 * this viewer ([savedArtworkIds]) decide whether that state belongs to this page.
 */
internal fun referenceSaveButton(
    image: Image?,
    state: SaveReferenceState,
    submittedArtworkId: String?,
    savedArtworkIds: Set<String>,
): ReferenceSaveButton {
    val artworkId = image?.sourceReference?.artworkId ?: return ReferenceSaveButton.Hidden
    if (artworkId in savedArtworkIds) return ReferenceSaveButton.Saved
    return when (state) {
        is SaveReferenceState.Saved ->
            if (state.artworkId == artworkId) ReferenceSaveButton.Saved else ReferenceSaveButton.Ready
        SaveReferenceState.Saving ->
            // The view model accepts one save at a time, so other pages wait instead of queueing.
            if (submittedArtworkId == artworkId) ReferenceSaveButton.Saving else ReferenceSaveButton.Busy
        SaveReferenceState.Failed ->
            if (submittedArtworkId == artworkId) ReferenceSaveButton.Failed else ReferenceSaveButton.Ready
        SaveReferenceState.Idle, SaveReferenceState.NoSource -> ReferenceSaveButton.Ready
    }
}

@Stable
internal class ReferenceSaveTracker {
    var submittedArtworkId by mutableStateOf<String?>(null)
    var savedArtworkIds by mutableStateOf<Set<String>>(emptySet())

    fun buttonFor(image: Image?, saving: ReferenceSaving?): ReferenceSaveButton =
        if (saving == null) {
            ReferenceSaveButton.Hidden
        } else {
            referenceSaveButton(image, saving.state, submittedArtworkId, savedArtworkIds)
        }

    fun save(image: Image, saving: ReferenceSaving) {
        submittedArtworkId = image.sourceReference?.artworkId
        saving.onSave(image)
    }
}

@Composable
fun referenceSavingOf(viewModel: SavedReferenceViewModel): ReferenceSaving {
    val state by viewModel.saveState.collectAsStateWithLifecycle()
    return ReferenceSaving(state, viewModel::saveImage)
}

@Composable
internal fun rememberReferenceSaveTracker(saving: ReferenceSaving?): ReferenceSaveTracker {
    val tracker = remember { ReferenceSaveTracker() }
    val saved = saving?.state as? SaveReferenceState.Saved
    LaunchedEffect(saved) {
        if (saved != null) tracker.savedArtworkIds += saved.artworkId
    }
    return tracker
}

@Composable
internal fun SaveReferenceButton(button: ReferenceSaveButton, onClick: () -> Unit) {
    if (button == ReferenceSaveButton.Hidden) return
    val container = MaterialTheme.colorScheme.surface.copy(alpha = CONTROL_ALPHA)
    val content = MaterialTheme.colorScheme.onSurface
    Button(
        onClick = onClick,
        enabled = button == ReferenceSaveButton.Ready || button == ReferenceSaveButton.Failed,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container,
            disabledContentColor = content.copy(alpha = DISABLED_ALPHA),
        ),
    ) {
        when (button) {
            ReferenceSaveButton.Saving ->
                CircularProgressIndicator(modifier = Modifier.size(ICON_SIZE), strokeWidth = 2.dp)
            ReferenceSaveButton.Saved -> Icon(Icons.Filled.Bookmark, contentDescription = null)
            ReferenceSaveButton.Failed -> Icon(Icons.Filled.Refresh, contentDescription = null)
            else -> Icon(Icons.Outlined.BookmarkBorder, contentDescription = null)
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = stringResource(
                when (button) {
                    ReferenceSaveButton.Saving -> R.string.saved_reference_saving
                    ReferenceSaveButton.Saved -> R.string.saved_reference_saved
                    ReferenceSaveButton.Failed -> R.string.saved_reference_retry
                    else -> R.string.saved_reference_save_locally
                },
            ),
        )
    }
}

private const val CONTROL_ALPHA = 0.7f
private const val DISABLED_ALPHA = 0.7f
private val ICON_SIZE = 18.dp
