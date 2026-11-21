package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.model.SourceReference
import kotlinx.coroutines.flow.Flow

/** Saves artwork source references into the Saved Prompts library and reads them back by ID. */
interface SavedReferenceRepository {
    /**
     * Stores [reference] as a new saved prompt and returns its local ID. The editable prompt
     * fields start from the source's generation settings, or empty when it supplied none.
     */
    suspend fun saveReference(reference: SourceReference): Long

    suspend fun getReference(id: Long): SavedPrompt?

    fun observeReference(id: Long): Flow<SavedPrompt?>
}
