package com.riox432.civitdeck.feature.detail.presentation

import com.riox432.civitdeck.domain.usecase.AddPersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.DeleteModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.RemovePersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.SaveModelNoteUseCase
import com.riox432.civitdeck.domain.util.launchSafe
import kotlinx.coroutines.CoroutineScope

private const val TAG = "DetailNotesTagsDelegate"

internal class DetailNotesTagsDelegate(
    private val modelId: Long,
    private val scope: CoroutineScope,
    private val saveModelNoteUseCase: SaveModelNoteUseCase,
    private val deleteModelNoteUseCase: DeleteModelNoteUseCase,
    private val addPersonalTagUseCase: AddPersonalTagUseCase,
    private val removePersonalTagUseCase: RemovePersonalTagUseCase,
) {

    fun saveNote(text: String) {
        scope.launchSafe(TAG, "Note save") {
            if (text.isBlank()) {
                deleteModelNoteUseCase(modelId)
            } else {
                saveModelNoteUseCase(modelId, text)
            }
        }
    }

    fun addTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank()) return
        scope.launchSafe(TAG, "Add tag") { addPersonalTagUseCase(modelId, trimmed) }
    }

    fun removeTag(tag: String) {
        scope.launchSafe(TAG, "Remove tag") { removePersonalTagUseCase(modelId, tag) }
    }
}
