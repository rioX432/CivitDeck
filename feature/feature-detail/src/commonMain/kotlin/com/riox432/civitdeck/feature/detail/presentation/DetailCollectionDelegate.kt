package com.riox432.civitdeck.feature.detail.presentation

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.usecase.AddModelToCollectionUseCase
import com.riox432.civitdeck.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelFromCollectionUseCase
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "DetailCollectionDelegate"

internal class DetailCollectionDelegate(
    private val scope: CoroutineScope,
    private val modelCollectionIds: StateFlow<List<Long>>,
    private val addModelToCollectionUseCase: AddModelToCollectionUseCase,
    private val removeModelFromCollectionUseCase: RemoveModelFromCollectionUseCase,
    private val createCollectionUseCase: CreateCollectionUseCase,
) {

    fun toggleCollection(collectionId: Long, model: Model?) {
        model ?: return
        scope.launch {
            suspendRunCatching {
                if (collectionId in modelCollectionIds.value) {
                    removeModelFromCollectionUseCase(collectionId, model.id)
                } else {
                    addModelToCollectionUseCase(collectionId, model)
                }
            }.onFailure { e -> Logger.w(TAG, "Collection toggle failed: ${e.message}") }
        }
    }

    fun createCollectionAndAdd(name: String, model: Model?) {
        model ?: return
        scope.launch {
            suspendRunCatching {
                val newId = createCollectionUseCase(name)
                addModelToCollectionUseCase(newId, model)
            }.onFailure { e -> Logger.w(TAG, "Create collection and add failed: ${e.message}") }
        }
    }
}
