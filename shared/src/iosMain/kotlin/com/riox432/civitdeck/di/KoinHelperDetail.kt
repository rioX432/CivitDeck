@file:Suppress("TooManyFunctions", "MaxLineLength")

package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddPersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.CleanupBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.DeleteBrowsingHistoryItemUseCase
import com.riox432.civitdeck.domain.usecase.DeleteModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.FollowCreatorUseCase
import com.riox432.civitdeck.domain.usecase.GetAllPersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetBrowsingStatsUseCase
import com.riox432.civitdeck.domain.usecase.GetCreatorFeedUseCase
import com.riox432.civitdeck.domain.usecase.GetFollowedCreatorsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.GetModelLicenseUseCase
import com.riox432.civitdeck.domain.usecase.GetModelReviewsUseCase
import com.riox432.civitdeck.domain.usecase.GetRatingTotalsUseCase
import com.riox432.civitdeck.domain.usecase.GetUnreadFeedCountUseCase
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.IsFollowingCreatorUseCase
import com.riox432.civitdeck.domain.usecase.MarkFeedReadUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ObservePersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveRecentlyViewedUseCase
import com.riox432.civitdeck.domain.usecase.RemovePersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.SaveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.SearchModelsByTagUseCase
import com.riox432.civitdeck.domain.usecase.SubmitReviewUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import com.riox432.civitdeck.domain.usecase.UnfollowCreatorUseCase
import com.riox432.civitdeck.domain.usecase.UnhideModelUseCase
import com.riox432.civitdeck.feature.creator.domain.usecase.GetCreatorModelsUseCase
import com.riox432.civitdeck.feature.gallery.domain.usecase.GetImagesUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetHiddenModelIdsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.HideModelUseCase

// Model Detail & Creator
fun KoinHelper.getModelDetailUseCase(): GetModelDetailUseCase = resolve()
fun KoinHelper.getCreatorModelsUseCase(): GetCreatorModelsUseCase = resolve()
fun KoinHelper.getImagesUseCase(): GetImagesUseCase = resolve()
fun KoinHelper.getModelLicenseUseCase(): GetModelLicenseUseCase = resolve()

// Browsing History
fun KoinHelper.getTrackModelViewUseCase(): TrackModelViewUseCase = resolve()
fun KoinHelper.getObserveRecentlyViewedUseCase(): ObserveRecentlyViewedUseCase = resolve()
fun KoinHelper.getViewedModelIdsUseCase(): GetViewedModelIdsUseCase = resolve()
fun KoinHelper.getClearBrowsingHistoryUseCase(): ClearBrowsingHistoryUseCase = resolve()
fun KoinHelper.getDeleteBrowsingHistoryItemUseCase(): DeleteBrowsingHistoryItemUseCase = resolve()
fun KoinHelper.getCleanupBrowsingHistoryUseCase(): CleanupBrowsingHistoryUseCase = resolve()

// Hidden Models
fun KoinHelper.getHiddenModelIdsUseCase(): GetHiddenModelIdsUseCase = resolve()
fun KoinHelper.getHideModelUseCase(): HideModelUseCase = resolve()
fun KoinHelper.getUnhideModelUseCase(): UnhideModelUseCase = resolve()
fun KoinHelper.getHiddenModelsUseCase(): GetHiddenModelsUseCase = resolve()

// Model Notes & Personal Tags
fun KoinHelper.getObserveModelNoteUseCase(): ObserveModelNoteUseCase = resolve()
fun KoinHelper.getSaveModelNoteUseCase(): SaveModelNoteUseCase = resolve()
fun KoinHelper.getDeleteModelNoteUseCase(): DeleteModelNoteUseCase = resolve()
fun KoinHelper.getObservePersonalTagsUseCase(): ObservePersonalTagsUseCase = resolve()
fun KoinHelper.getAddPersonalTagUseCase(): AddPersonalTagUseCase = resolve()
fun KoinHelper.getRemovePersonalTagUseCase(): RemovePersonalTagUseCase = resolve()
fun KoinHelper.getGetAllPersonalTagsUseCase(): GetAllPersonalTagsUseCase = resolve()
fun KoinHelper.getSearchModelsByTagUseCase(): SearchModelsByTagUseCase = resolve()

// Analytics
fun KoinHelper.getBrowsingStatsUseCase(): GetBrowsingStatsUseCase = resolve()

// Reviews
fun KoinHelper.getModelReviewsUseCase(): GetModelReviewsUseCase = resolve()
fun KoinHelper.getRatingTotalsUseCase(): GetRatingTotalsUseCase = resolve()
fun KoinHelper.getSubmitReviewUseCase(): SubmitReviewUseCase = resolve()

// Creator Follow & Feed
fun KoinHelper.getFollowCreatorUseCase(): FollowCreatorUseCase = resolve()
fun KoinHelper.getUnfollowCreatorUseCase(): UnfollowCreatorUseCase = resolve()
fun KoinHelper.getIsFollowingCreatorUseCase(): IsFollowingCreatorUseCase = resolve()
fun KoinHelper.getCreatorFeedUseCase(): GetCreatorFeedUseCase = resolve()
fun KoinHelper.getUnreadFeedCountUseCase(): GetUnreadFeedCountUseCase = resolve()
fun KoinHelper.getMarkFeedReadUseCase(): MarkFeedReadUseCase = resolve()
fun KoinHelper.getFollowedCreatorsUseCase(): GetFollowedCreatorsUseCase = resolve()
