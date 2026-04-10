package com.riox432.civitdeck.feature.detail.presentation

import com.riox432.civitdeck.domain.usecase.AddPersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.DeleteModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.RemovePersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.SaveModelNoteUseCase
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
        scope.launch {
            suspendRunCatching {
                if (text.isBlank()) {
                    deleteModelNoteUseCase(modelId)
                } else {
                    saveModelNoteUseCase(modelId, text)
                }
            }.onFailure { e -> Logger.w(TAG, "Note save failed: ${e.message}") }
        }
    }

    fun addTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank()) return
        scope.launch {
            suspendRunCatching { addPersonalTagUseCase(modelId, trimmed) }
                .onFailure { e -> Logger.w(TAG, "Add tag failed: ${e.message}") }
        }
    }

    fun removeTag(tag: String) {
        scope.launch {
            suspendRunCatching { removePersonalTagUseCase(modelId, tag) }
                .onFailure { e -> Logger.w(TAG, "Remove tag failed: ${e.message}") }
        }
    }
}
