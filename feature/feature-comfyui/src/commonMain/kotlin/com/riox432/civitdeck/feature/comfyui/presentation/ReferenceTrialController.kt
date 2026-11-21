package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.ComfyUIJobTarget
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.domain.model.EffectiveGenerationSettings
import com.riox432.civitdeck.domain.model.PreparedReferenceGeneration
import com.riox432.civitdeck.domain.model.ReferenceResourceReview
import com.riox432.civitdeck.domain.model.ResourceConfidence
import com.riox432.civitdeck.domain.model.SourceGenerationSnapshot
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.domain.model.SourceReferenceDecodeResult
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.domain.repository.SavedReferenceRepository
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PrepareReferenceGenerationUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ReviewReferenceResourcesUseCase
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What a reference trial needs beyond ordinary generation; absent, the form only generates manually. */
class ReferenceTrialDependencies(
    val savedReferences: SavedReferenceRepository,
    val prepare: PrepareReferenceGenerationUseCase,
    val review: ReferenceReviewer,
)

/** Reviews a prepared reference; [ReviewReferenceResourcesUseCase] in production. */
fun interface ReferenceReviewer {
    suspend fun review(prepared: PreparedReferenceGeneration, source: SourceReference): ReferenceResourceReview
}

fun ReviewReferenceResourcesUseCase.asReviewer() = ReferenceReviewer { prepared, source -> this(prepared, source) }

enum class ReferenceTrialStatus { DRAFT, PREPARING, REVIEWED, SUBMITTED }

enum class ReferenceTrialProblem {
    /** The saved prompt is missing or holds no readable source reference. */
    SOURCE_UNAVAILABLE,

    /** The form cannot generate yet, for example without a prompt or model. */
    INCOMPLETE_DRAFT,

    NO_ACTIVE_CONNECTION,
    PREPARATION_FAILED,
}

/**
 * A saved reference linked to the generation form. Null only when no reference is linked;
 * closing the review keeps it, so the form cannot fall back to unreviewed Generate.
 *
 * [source] is the immutable reference as saved; its null generation fields were not recorded,
 * so the form values standing in for them are defaults, not source values.
 */
data class ReferenceReviewState(
    val referenceId: Long,
    val source: SourceReference? = null,
    val isLoadingSource: Boolean = true,
    val isReviewVisible: Boolean = false,
    val status: ReferenceTrialStatus = ReferenceTrialStatus.DRAFT,
    val target: ComfyUIJobTarget? = null,
    val effectiveSettings: EffectiveGenerationSettings? = null,
    val review: ReferenceResourceReview? = null,
    val variationAcknowledged: Boolean = false,
    val problem: ReferenceTrialProblem? = null,
    val errorDetail: String? = null,
) {
    val canConfirm: Boolean
        get() {
            val review = review ?: return false
            return status == ReferenceTrialStatus.REVIEWED &&
                review.canConfirm &&
                review.resources.none { it.selected && it.confidence == ResourceConfidence.MISSING } &&
                (variationAcknowledged || !review.requiresVariationAcknowledgement)
        }
}

/**
 * Gates generation from a saved reference behind a review of the exact payload that will be
 * sent. The payload is prepared once from the form's inputs and one captured target; any later
 * input change discards it, so what is confirmed is what was reviewed.
 */
internal class ReferenceTrialController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<GenerationUiState>,
    private val dependencies: ReferenceTrialDependencies?,
    private val connections: ComfyUIConnectionRepository,
    private val generationInputs: (GenerationUiState) -> ComfyUIGenerationParams,
    private val submitPrepared: (PreparedReferenceGeneration) -> Unit,
) {
    private val _state = MutableStateFlow<ReferenceReviewState?>(null)
    val state: StateFlow<ReferenceReviewState?> = _state

    val isLinked: Boolean get() = _state.value != null

    // Bumped on every begin, edit, dismiss and clear, so a late load or review sees it is stale.
    private var revision = 0L
    private var work: Job? = null
    private var frozenInputs: ComfyUIGenerationParams? = null
    private var prepared: PreparedReferenceGeneration? = null

    init {
        // Progress, preview and toasts are not generation inputs, so they never reach here.
        scope.launch {
            uiState.collect { ui ->
                val frozen = frozenInputs ?: return@collect
                if (generationInputs(ui) != frozen) discardReview()
            }
        }
    }

    /**
     * Links [referenceId] after [resetDraft] has cleared the form, then hands the recorded
     * generation values to [applySource] once the reference loads.
     */
    fun begin(referenceId: Long, resetDraft: () -> Unit, applySource: (SourceGenerationSnapshot) -> Unit) {
        val deps = dependencies ?: return
        cancelWork()
        resetDraft()
        _state.value = ReferenceReviewState(referenceId)
        val started = revision
        work = scope.launch {
            val source = loadSource(deps, referenceId)
            if (revision != started) return@launch
            source?.generation?.let(applySource)
            _state.update {
                it?.copy(
                    source = source,
                    isLoadingSource = false,
                    problem = if (source == null) ReferenceTrialProblem.SOURCE_UNAVAILABLE else null,
                )
            }
        }
    }

    fun prepare() {
        val deps = dependencies ?: return
        val current = _state.value ?: return
        val source = current.source
        if (current.status == ReferenceTrialStatus.PREPARING || source == null) return
        val ui = uiState.value
        if (!ui.canGenerate) {
            _state.value = current.copy(problem = ReferenceTrialProblem.INCOMPLETE_DRAFT)
            return
        }
        cancelWork()
        val inputs = generationInputs(ui)
        frozenInputs = inputs
        _state.value = current.withoutReview().copy(
            isReviewVisible = true,
            status = ReferenceTrialStatus.PREPARING,
        )
        val started = revision
        work = scope.launch { runPreparation(deps, current.referenceId, source, inputs, started) }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runPreparation(
        deps: ReferenceTrialDependencies,
        referenceId: Long,
        source: SourceReference,
        inputs: ComfyUIGenerationParams,
        started: Long,
    ) {
        try {
            val active = connections.getActiveConnection()
                ?: throw DomainException.ConnectionException("No active ComfyUI connection")
            val target = ComfyUIJobTarget.from(active)
            val payload = deps.prepare(referenceId, target, inputs)
            val review = deps.review.review(payload, source)
            if (revision != started) return
            prepared = payload
            _state.update {
                it?.copy(
                    status = ReferenceTrialStatus.REVIEWED,
                    target = target,
                    effectiveSettings = payload.effectiveSettings,
                    review = review,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: DomainException.ConnectionException) {
            failPreparation(started, ReferenceTrialProblem.NO_ACTIVE_CONNECTION, e)
        } catch (e: Exception) {
            failPreparation(started, ReferenceTrialProblem.PREPARATION_FAILED, e)
        }
    }

    private fun failPreparation(started: Long, problem: ReferenceTrialProblem, e: Exception) {
        Logger.e(TAG, "Reference preparation failed: ${e.message}")
        if (revision != started) return
        frozenInputs = null
        _state.update {
            it?.copy(status = ReferenceTrialStatus.DRAFT, problem = problem, errorDetail = e.message)
        }
    }

    fun acknowledgeVariation(acknowledged: Boolean) {
        _state.update {
            if (it?.status == ReferenceTrialStatus.REVIEWED) it.copy(variationAcknowledged = acknowledged) else it
        }
    }

    /** Sends the reviewed payload once; anything changed since review discards it instead. */
    fun confirm() {
        val current = _state.value ?: return
        val payload = prepared ?: return
        if (!current.canConfirm) return
        if (generationInputs(uiState.value) != frozenInputs) {
            discardReview()
            return
        }
        prepared = null
        frozenInputs = null
        _state.value = current.copy(status = ReferenceTrialStatus.SUBMITTED, isReviewVisible = false)
        submitPrepared(payload)
    }

    fun dismiss() {
        cancelWork()
        _state.update { it?.withoutReview()?.copy(status = ReferenceTrialStatus.DRAFT) }
    }

    fun clear() {
        cancelWork()
        _state.value = null
    }

    private fun discardReview() {
        cancelWork()
        _state.update {
            if (it == null || it.status == ReferenceTrialStatus.SUBMITTED) {
                it
            } else {
                it.withoutReview().copy(status = ReferenceTrialStatus.DRAFT)
            }
        }
    }

    private fun cancelWork() {
        revision++
        work?.cancel()
        work = null
        frozenInputs = null
        prepared = null
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadSource(deps: ReferenceTrialDependencies, referenceId: Long): SourceReference? = try {
        val decoded = deps.savedReferences.getReference(referenceId)?.sourceReference
        (decoded as? SourceReferenceDecodeResult.Decoded)?.reference
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.e(TAG, "Loading reference $referenceId failed: ${e.message}")
        null
    }

    private fun ReferenceReviewState.withoutReview() = copy(
        isReviewVisible = false,
        target = null,
        effectiveSettings = null,
        review = null,
        variationAcknowledged = false,
        problem = null,
        errorDetail = null,
    )

    private companion object {
        const val TAG = "ReferenceTrialController"
    }
}

/**
 * Fills the form with the values [gen] recorded and leaves the rest as they are. Sampler and
 * model are not set here: the sampler follows the selected model family, and the model needs
 * the server's file lists.
 */
internal fun GenerationUiState.withSourceValues(gen: SourceGenerationSnapshot): GenerationUiState {
    val size = gen.size?.let(::parseSize)
    return copy(
        prompt = gen.prompt ?: prompt,
        negativePrompt = gen.negativePrompt ?: negativePrompt,
        steps = gen.steps ?: steps,
        cfgScale = gen.cfgScale ?: cfgScale,
        seed = gen.seed ?: seed,
        width = size?.first ?: width,
        height = size?.second ?: height,
    )
}

private fun parseSize(size: String): Pair<Int, Int>? {
    val parts = size.lowercase().replace(" ", "").split('x')
    if (parts.size != 2) return null
    val width = parts[0].toIntOrNull()?.takeIf { it > 0 } ?: return null
    val height = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return width to height
}
