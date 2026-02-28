package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.image.ImageSaver
import com.riox432.civitdeck.data.repository.AuthRepositoryImpl
import com.riox432.civitdeck.data.repository.BrowsingHistoryRepositoryImpl
import com.riox432.civitdeck.data.repository.CacheRepositoryImpl
import com.riox432.civitdeck.data.repository.FavoriteRepositoryImpl
import com.riox432.civitdeck.data.repository.LocalModelFileRepositoryImpl
import com.riox432.civitdeck.data.repository.ModelRepositoryImpl
import com.riox432.civitdeck.data.repository.ModelVersionCheckpointRepositoryImpl
import com.riox432.civitdeck.data.repository.TagRepositoryImpl
import com.riox432.civitdeck.data.scanner.FileScanner
import com.riox432.civitdeck.domain.repository.AuthRepository
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import com.riox432.civitdeck.domain.repository.CacheRepository
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.LocalModelFileRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.ModelVersionCheckpointRepository
import com.riox432.civitdeck.domain.repository.TagRepository
import org.koin.dsl.module

val dataModule = module {
    // File Scanner
    single { FileScanner() }

    // Image Saver (platform-specific via expect/actual)
    single { ImageSaver() }

    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<CacheRepository> { CacheRepositoryImpl(get()) }
    single<ModelRepository> { ModelRepositoryImpl(get(), get(), get()) }
    single<TagRepository> { TagRepositoryImpl(get()) }
    single<FavoriteRepository> { FavoriteRepositoryImpl(get()) }
    single<BrowsingHistoryRepository> { BrowsingHistoryRepositoryImpl(get()) }
    single<LocalModelFileRepository> { LocalModelFileRepositoryImpl(get(), get(), get()) }
    single<ModelVersionCheckpointRepository> { ModelVersionCheckpointRepositoryImpl(get()) }
}
