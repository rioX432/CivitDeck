package com.riox432.civitdeck.ui.comfyui

import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.ResourceConfidence
import com.riox432.civitdeck.domain.model.SourceProvider
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.feature.comfyui.presentation.ReferenceReviewState
import com.riox432.civitdeck.feature.comfyui.presentation.ReferenceTrialStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReferenceReviewSheetTest {

    private val loaded = ReferenceReviewState(
        referenceId = 1,
        source = SourceReference(
            provider = SourceProvider.Civitai,
            artworkId = "1",
            artworkUrl = "https://civitai.com/images/1",
        ),
        isLoadingSource = false,
    )

    @Test
    fun review_is_available_for_a_loaded_reference_on_an_idle_form() {
        assertTrue(canStartReview(loaded, GenerationStatus.Idle))
        assertTrue(canStartReview(loaded.copy(status = ReferenceTrialStatus.SUBMITTED), GenerationStatus.Completed))
    }

    @Test
    fun review_waits_for_the_source_a_running_preparation_and_a_running_job() {
        assertFalse(canStartReview(loaded.copy(isLoadingSource = true), GenerationStatus.Idle))
        assertFalse(canStartReview(loaded.copy(source = null), GenerationStatus.Idle))
        assertFalse(canStartReview(loaded.copy(status = ReferenceTrialStatus.PREPARING), GenerationStatus.Idle))
        assertFalse(canStartReview(loaded, GenerationStatus.Submitting))
        assertFalse(canStartReview(loaded, GenerationStatus.Running))
    }

    @Test
    fun every_confidence_has_its_own_label_so_filename_only_never_reads_as_verified() {
        val labels = ResourceConfidence.entries.map(::confidenceLabel)
        assertEquals(labels.size, labels.toSet().size)
    }
}
