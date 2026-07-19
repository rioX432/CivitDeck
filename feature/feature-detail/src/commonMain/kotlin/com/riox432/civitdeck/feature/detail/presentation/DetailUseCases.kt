package com.riox432.civitdeck.feature.detail.presentation

import com.riox432.civitdeck.domain.usecase.AddModelToCollectionUseCase
import com.riox432.civitdeck.domain.usecase.AddPersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.CancelDownloadUseCase
import com.riox432.civitdeck.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.EmbedOnBrowseUseCase
import com.riox432.civitdeck.domain.usecase.EnqueueDownloadUseCase
import com.riox432.civitdeck.domain.usecase.EnrichModelImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObservePersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelFromCollectionUseCase
import com.riox432.civitdeck.domain.usecase.RemovePersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.SaveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase

/**
 * Core model operations: loading, favorites, tracking, enrichment, and settings observers.
 */
data class ModelUseCases(
    val getModelDetail: GetModelDetailUseCase,
    val observeIsFavorite: ObserveIsFavoriteUseCase,
    val toggleFavorite: ToggleFavoriteUseCase,
    val trackModelView: TrackModelViewUseCase,
    val enrichModelImages: EnrichModelImagesUseCase,
    val embedOnBrowse: EmbedOnBrowseUseCase,
    val observeNsfwFilter: ObserveNsfwFilterUseCase,
    val observePowerUserMode: ObservePowerUserModeUseCase,
)

/**
 * Collection management: observing, adding/removing models, creating collections.
 */
data class CollectionUseCases(
    val observeCollections: ObserveCollectionsUseCase,
    val observeModelCollections: ObserveModelCollectionsUseCase,
    val addModelToCollection: AddModelToCollectionUseCase,
    val removeModelFromCollection: RemoveModelFromCollectionUseCase,
    val createCollection: CreateCollectionUseCase,
)

/**
 * Notes and personal tags: observing, saving/deleting notes, adding/removing tags.
 */
data class NotesTagsUseCases(
    val observeModelNote: ObserveModelNoteUseCase,
    val saveModelNote: SaveModelNoteUseCase,
    val deleteModelNote: DeleteModelNoteUseCase,
    val observePersonalTags: ObservePersonalTagsUseCase,
    val addPersonalTag: AddPersonalTagUseCase,
    val removePersonalTag: RemovePersonalTagUseCase,
)

/**
 * Download management: observing download status, enqueueing, and cancelling.
 */
data class DownloadUseCases(
    val observeModelDownloads: ObserveModelDownloadsUseCase,
    val enqueueDownload: EnqueueDownloadUseCase,
    val cancelDownload: CancelDownloadUseCase,
)
