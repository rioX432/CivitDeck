package com.riox432.civitdeck.feature.prompts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.domain.model.ModelFile
import com.riox432.civitdeck.domain.model.ModelReference
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.domain.model.SourceReferenceDecodeResult
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.SavedReferenceRepository
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SaveReferenceState {
    data object Idle : SaveReferenceState
    data object Saving : SaveReferenceState
    data class Saved(val id: Long, val artworkId: String) : SaveReferenceState

    /** The image carried no valid source reference, so there is nothing to save. */
    data object NoSource : SaveReferenceState

    /** Writing failed; saving again is allowed. */
    data object Failed : SaveReferenceState
}

sealed interface SavedReferenceDetailState {
    data object Idle : SavedReferenceDetailState
    data object Loading : SavedReferenceDetailState

    data class Loaded(val prompt: SavedPrompt) : SavedReferenceDetailState {
        /** The stored snapshot, or null when there is none or this build cannot read it. */
        val reference: SourceReference?
            get() = (prompt.sourceReference as? SourceReferenceDecodeResult.Decoded)?.reference

        /** A snapshot is stored but is corrupt or from a newer version; the prompt stays usable. */
        val sourceUnavailable: Boolean
            get() = prompt.sourceReference != null && reference == null
    }

    data object NotFound : SavedReferenceDetailState
    data object Failed : SavedReferenceDetailState
}

sealed interface ResourceVersionState {
    val versionId: Long

    data class Loading(override val versionId: Long) : ResourceVersionState

    data class Available(
        override val versionId: Long,
        val modelId: Long,
        val name: String,
        val baseModel: String?,
        val files: List<ModelFile>,
        val url: String,
    ) : ResourceVersionState

    /** Missing, deleted, gated or unreachable. The saved snapshot still names [versionId]. */
    data class Unavailable(override val versionId: Long) : ResourceVersionState
}

/**
 * Saves an artwork's source reference into the Saved Prompts library and inspects what it
 * recorded, for Android, iOS and Desktop alike.
 */
class SavedReferenceViewModel(
    private val savedReferenceRepository: SavedReferenceRepository,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    private val _saveState = MutableStateFlow<SaveReferenceState>(SaveReferenceState.Idle)
    val saveState: StateFlow<SaveReferenceState> = _saveState.asStateFlow()

    private val _detailState = MutableStateFlow<SavedReferenceDetailState>(SavedReferenceDetailState.Idle)
    val detailState: StateFlow<SavedReferenceDetailState> = _detailState.asStateFlow()

    private val _resources = MutableStateFlow<Map<Long, ResourceVersionState>>(emptyMap())

    /** Inspected resource versions keyed by the recorded model-version ID. */
    val resources: StateFlow<Map<Long, ResourceVersionState>> = _resources.asStateFlow()

    private var detailJob: Job? = null

    fun saveImage(image: Image) {
        val reference = image.sourceReference
        if (reference == null) {
            _saveState.value = SaveReferenceState.NoSource
            return
        }
        val current = _saveState.value
        // Repeated taps must not write the same artwork twice.
        if (current is SaveReferenceState.Saving) return
        if (current is SaveReferenceState.Saved && current.artworkId == reference.artworkId) return
        _saveState.value = SaveReferenceState.Saving
        viewModelScope.launch {
            _saveState.value = suspendRunCatching { savedReferenceRepository.saveReference(reference) }
                .fold(
                    onSuccess = { SaveReferenceState.Saved(it, reference.artworkId) },
                    onFailure = { e ->
                        Logger.w(TAG, "Saving reference failed: ${e.message}")
                        SaveReferenceState.Failed
                    },
                )
        }
    }

    fun loadReference(id: Long) {
        detailJob?.cancel()
        _detailState.value = SavedReferenceDetailState.Loading
        detailJob = viewModelScope.launch {
            savedReferenceRepository.observeReference(id)
                .catch { e ->
                    Logger.w(TAG, "Loading reference $id failed: ${e.message}")
                    _detailState.value = SavedReferenceDetailState.Failed
                }
                .collect { prompt ->
                    _detailState.value = prompt?.let { SavedReferenceDetailState.Loaded(it) }
                        ?: SavedReferenceDetailState.NotFound
                }
        }
    }

    /** Looks up exactly [recordedVersionId]; it never falls back to another version of the model. */
    fun inspectResource(recordedVersionId: Long) {
        val current = _resources.value[recordedVersionId]
        if (current is ResourceVersionState.Loading || current is ResourceVersionState.Available) return
        if (recordedVersionId <= 0) {
            putResource(ResourceVersionState.Unavailable(recordedVersionId))
            return
        }
        putResource(ResourceVersionState.Loading(recordedVersionId))
        viewModelScope.launch {
            val version = suspendRunCatching { modelRepository.getModelVersion(recordedVersionId) }
                .onFailure { e -> Logger.w(TAG, "Resource version $recordedVersionId unavailable: ${e.message}") }
                .getOrNull()
                ?.takeIf { it.id == recordedVersionId }
            putResource(
                version?.let {
                    ResourceVersionState.Available(
                        versionId = recordedVersionId,
                        modelId = it.modelId,
                        name = it.name,
                        baseModel = it.baseModel,
                        files = it.files,
                        url = civitaiVersionUrl(it.modelId, recordedVersionId),
                    )
                } ?: ResourceVersionState.Unavailable(recordedVersionId),
            )
        }
    }

    private fun putResource(state: ResourceVersionState) {
        _resources.update { it + (state.versionId to state) }
    }

    private companion object {
        const val TAG = "SavedReferenceVM"

        // CivitAI's own version link form: /models/{modelId}?modelVersionId={versionId}.
        fun civitaiVersionUrl(modelId: Long, versionId: Long): String =
            "${ModelReference.civitai(modelId).canonicalUrl}?modelVersionId=$versionId"
    }
}
