package com.riox432.civitdeck.ui.gallery

import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.domain.model.MediaContentType
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.SourceProvider
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.feature.prompts.presentation.SaveReferenceState
import kotlin.test.Test
import kotlin.test.assertEquals

class ReferenceSaveButtonTest {

    private val first = image("101")
    private val second = image("202")

    @Test
    fun an_image_without_a_source_reference_offers_no_save() {
        val button = referenceSaveButton(image(null), SaveReferenceState.Idle, null, emptySet())

        assertEquals(ReferenceSaveButton.Hidden, button)
    }

    @Test
    fun a_legacy_url_only_page_offers_no_save() {
        assertEquals(ReferenceSaveButton.Hidden, referenceSaveButton(null, SaveReferenceState.Idle, null, emptySet()))
    }

    @Test
    fun a_metadata_free_image_with_a_reference_can_be_saved() {
        val button = referenceSaveButton(first, SaveReferenceState.Idle, null, emptySet())

        assertEquals(ReferenceSaveButton.Ready, button)
    }

    @Test
    fun an_in_flight_save_shows_on_its_page_and_blocks_other_pages() {
        val state = SaveReferenceState.Saving

        assertEquals(ReferenceSaveButton.Saving, referenceSaveButton(first, state, "101", emptySet()))
        assertEquals(ReferenceSaveButton.Busy, referenceSaveButton(second, state, "101", emptySet()))
    }

    @Test
    fun a_success_is_not_shown_on_the_next_page() {
        val state = SaveReferenceState.Saved(id = 7L, artworkId = "101")

        assertEquals(ReferenceSaveButton.Saved, referenceSaveButton(first, state, "101", emptySet()))
        assertEquals(ReferenceSaveButton.Ready, referenceSaveButton(second, state, "101", emptySet()))
    }

    @Test
    fun a_failure_is_retryable_only_on_the_page_that_submitted_it() {
        val state = SaveReferenceState.Failed

        assertEquals(ReferenceSaveButton.Failed, referenceSaveButton(first, state, "101", emptySet()))
        assertEquals(ReferenceSaveButton.Ready, referenceSaveButton(second, state, "101", emptySet()))
    }

    @Test
    fun an_earlier_saved_page_stays_saved_after_another_save() {
        val state = SaveReferenceState.Saved(id = 8L, artworkId = "202")

        assertEquals(ReferenceSaveButton.Saved, referenceSaveButton(first, state, "202", setOf("101")))
        assertEquals(
            ReferenceSaveButton.Saved,
            referenceSaveButton(first, SaveReferenceState.Saving, "202", setOf("101")),
        )
    }

    @Test
    fun the_tracker_hides_the_action_without_a_presenter_and_records_the_submitted_page() {
        val tracker = ReferenceSaveTracker()
        val saved = mutableListOf<Image>()
        val saving = ReferenceSaving(SaveReferenceState.Saving) { saved += it }

        assertEquals(ReferenceSaveButton.Hidden, tracker.buttonFor(first, null))
        tracker.save(second, saving)

        assertEquals(listOf(second), saved)
        assertEquals(ReferenceSaveButton.Saving, tracker.buttonFor(second, saving))
        assertEquals(ReferenceSaveButton.Busy, tracker.buttonFor(first, saving))
    }

    private fun image(artworkId: String?) = Image(
        id = artworkId?.toLong() ?: 0L,
        url = "https://image.civitai.com/x/${artworkId ?: "none"}.jpeg",
        hash = null,
        width = 512,
        height = 512,
        nsfw = false,
        nsfwLevel = NsfwLevel.None,
        createdAt = "2026-11-01",
        postId = null,
        username = "creator",
        stats = null,
        meta = null,
        contentType = MediaContentType.IMAGE,
        sourceReference = artworkId?.let {
            SourceReference(
                provider = SourceProvider.Civitai,
                artworkId = it,
                artworkUrl = "https://civitai.com/images/$it",
            )
        },
    )
}
