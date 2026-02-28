package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.repository.AuthRepositoryImpl
import com.riox432.civitdeck.data.repository.BrowsingHistoryRepositoryImpl
import com.riox432.civitdeck.data.repository.CacheRepositoryImpl
import com.riox432.civitdeck.data.repository.CollectionRepositoryImpl
import com.riox432.civitdeck.data.repository.ComfyUIRepositoryImpl
import com.riox432.civitdeck.data.repository.CreatorRepositoryImpl
import com.riox432.civitdeck.data.repository.ExcludedTagRepositoryImpl
import com.riox432.civitdeck.data.repository.FavoriteRepositoryImpl
import com.riox432.civitdeck.data.repository.HiddenModelRepositoryImpl
import com.riox432.civitdeck.data.repository.ImageRepositoryImpl
import com.riox432.civitdeck.data.repository.LocalModelFileRepositoryImpl
import com.riox432.civitdeck.data.repository.ModelRepositoryImpl
import com.riox432.civitdeck.data.repository.ModelVersionCheckpointRepositoryImpl
import com.riox432.civitdeck.data.repository.SavedPromptRepositoryImpl
import com.riox432.civitdeck.data.repository.SearchHistoryRepositoryImpl
import com.riox432.civitdeck.data.repository.TagRepositoryImpl
import com.riox432.civitdeck.data.repository.UserPreferencesRepositoryImpl
import com.riox432.civitdeck.data.scanner.FileScanner
import com.riox432.civitdeck.domain.repository.AuthRepository
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import com.riox432.civitdeck.domain.repository.CacheRepository
import com.riox432.civitdeck.domain.repository.CollectionRepository
import com.riox432.civitdeck.domain.repository.ComfyUIRepository
import com.riox432.civitdeck.domain.repository.CreatorRepository
import com.riox432.civitdeck.domain.repository.ExcludedTagRepository
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.HiddenModelRepository
import com.riox432.civitdeck.domain.repository.ImageRepository
import com.riox432.civitdeck.domain.repository.LocalModelFileRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.ModelVersionCheckpointRepository
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import com.riox432.civitdeck.domain.repository.SearchHistoryRepository
import com.riox432.civitdeck.domain.repository.TagRepository
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import org.koin.dsl.module

val dataModule = module {
    // File Scanner
    single { FileScanner() }

    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<CacheRepository> { CacheRepositoryImpl(get()) }
    single<ModelRepository> { ModelRepositoryImpl(get(), get(), get()) }
    single<ImageRepository> { ImageRepositoryImpl(get(), get(), get()) }
    single<CreatorRepository> { CreatorRepositoryImpl(get()) }
    single<TagRepository> { TagRepositoryImpl(get()) }
    single<FavoriteRepository> { FavoriteRepositoryImpl(get()) }
    single<CollectionRepository> { CollectionRepositoryImpl(get()) }
    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(get(), get()) }
    single<SavedPromptRepository> { SavedPromptRepositoryImpl(get()) }
    single<SearchHistoryRepository> { SearchHistoryRepositoryImpl(get()) }
    single<BrowsingHistoryRepository> { BrowsingHistoryRepositoryImpl(get()) }
    single<ExcludedTagRepository> { ExcludedTagRepositoryImpl(get()) }
    single<HiddenModelRepository> { HiddenModelRepositoryImpl(get()) }
    single<LocalModelFileRepository> { LocalModelFileRepositoryImpl(get(), get(), get()) }
    single<ModelVersionCheckpointRepository> { ModelVersionCheckpointRepositoryImpl(get()) }
    single<ComfyUIRepository> { ComfyUIRepositoryImpl(get(), get()) }
}
