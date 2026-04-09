@file:Suppress("TooManyFunctions", "MaxLineLength")

package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddPersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.CleanupBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.DeleteBrowsingHistoryItemUseCase
import com.riox432.civitdeck.domain.usecase.DeleteModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.GetAllPersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetBrowsingStatsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.GetModelLicenseUseCase
import com.riox432.civitdeck.domain.usecase.GetModelReviewsUseCase
import com.riox432.civitdeck.domain.usecase.GetRatingTotalsUseCase
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ObservePersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveRecentlyViewedUseCase
import com.riox432.civitdeck.domain.usecase.RemovePersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.SaveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.SearchModelsByTagUseCase
import com.riox432.civitdeck.domain.usecase.SubmitReviewUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import com.riox432.civitdeck.domain.usecase.UnhideModelUseCase
import com.riox432.civitdeck.feature.creator.domain.usecase.GetCreatorModelsUseCase
import com.riox432.civitdeck.feature.gallery.domain.usecase.GetImagesUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetHiddenModelIdsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.HideModelUseCase
import org.koin.mp.KoinPlatform.getKoin

// Model Detail & Creator
fun KoinHelper.getModelDetailUseCase(): GetModelDetailUseCase = getKoin().get()
fun KoinHelper.getCreatorModelsUseCase(): GetCreatorModelsUseCase = getKoin().get()
fun KoinHelper.getImagesUseCase(): GetImagesUseCase = getKoin().get()
fun KoinHelper.getModelLicenseUseCase(): GetModelLicenseUseCase = getKoin().get()

// Browsing History
fun KoinHelper.getTrackModelViewUseCase(): TrackModelViewUseCase = getKoin().get()
fun KoinHelper.getObserveRecentlyViewedUseCase(): ObserveRecentlyViewedUseCase = getKoin().get()
fun KoinHelper.getViewedModelIdsUseCase(): GetViewedModelIdsUseCase = getKoin().get()
fun KoinHelper.getClearBrowsingHistoryUseCase(): ClearBrowsingHistoryUseCase = getKoin().get()
fun KoinHelper.getDeleteBrowsingHistoryItemUseCase(): DeleteBrowsingHistoryItemUseCase = getKoin().get()
fun KoinHelper.getCleanupBrowsingHistoryUseCase(): CleanupBrowsingHistoryUseCase = getKoin().get()

// Hidden Models
fun KoinHelper.getHiddenModelIdsUseCase(): GetHiddenModelIdsUseCase = getKoin().get()
fun KoinHelper.getHideModelUseCase(): HideModelUseCase = getKoin().get()
fun KoinHelper.getUnhideModelUseCase(): UnhideModelUseCase = getKoin().get()
fun KoinHelper.getHiddenModelsUseCase(): GetHiddenModelsUseCase = getKoin().get()

// Model Notes & Personal Tags
fun KoinHelper.getObserveModelNoteUseCase(): ObserveModelNoteUseCase = getKoin().get()
fun KoinHelper.getSaveModelNoteUseCase(): SaveModelNoteUseCase = getKoin().get()
fun KoinHelper.getDeleteModelNoteUseCase(): DeleteModelNoteUseCase = getKoin().get()
fun KoinHelper.getObservePersonalTagsUseCase(): ObservePersonalTagsUseCase = getKoin().get()
fun KoinHelper.getAddPersonalTagUseCase(): AddPersonalTagUseCase = getKoin().get()
fun KoinHelper.getRemovePersonalTagUseCase(): RemovePersonalTagUseCase = getKoin().get()
fun KoinHelper.getGetAllPersonalTagsUseCase(): GetAllPersonalTagsUseCase = getKoin().get()
fun KoinHelper.getSearchModelsByTagUseCase(): SearchModelsByTagUseCase = getKoin().get()

// Analytics
fun KoinHelper.getBrowsingStatsUseCase(): GetBrowsingStatsUseCase = getKoin().get()

// Reviews
fun KoinHelper.getModelReviewsUseCase(): GetModelReviewsUseCase = getKoin().get()
fun KoinHelper.getRatingTotalsUseCase(): GetRatingTotalsUseCase = getKoin().get()
fun KoinHelper.getSubmitReviewUseCase(): SubmitReviewUseCase = getKoin().get()

// Creator Follow & Feed
fun KoinHelper.getFollowCreatorUseCase(): com.riox432.civitdeck.domain.usecase.FollowCreatorUseCase = getKoin().get()
fun KoinHelper.getUnfollowCreatorUseCase(): com.riox432.civitdeck.domain.usecase.UnfollowCreatorUseCase = getKoin().get()
fun KoinHelper.getIsFollowingCreatorUseCase(): com.riox432.civitdeck.domain.usecase.IsFollowingCreatorUseCase = getKoin().get()
fun KoinHelper.getCreatorFeedUseCase(): com.riox432.civitdeck.domain.usecase.GetCreatorFeedUseCase = getKoin().get()
fun KoinHelper.getUnreadFeedCountUseCase(): com.riox432.civitdeck.domain.usecase.GetUnreadFeedCountUseCase = getKoin().get()
fun KoinHelper.getMarkFeedReadUseCase(): com.riox432.civitdeck.domain.usecase.MarkFeedReadUseCase = getKoin().get()
fun KoinHelper.getFollowedCreatorsUseCase(): com.riox432.civitdeck.domain.usecase.GetFollowedCreatorsUseCase = getKoin().get()
