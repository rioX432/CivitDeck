package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.repository.AuthRepositoryImpl
import com.riox432.civitdeck.data.repository.CreatorFollowRepositoryImpl
import com.riox432.civitdeck.data.repository.LocalModelFileRepositoryImpl
import com.riox432.civitdeck.data.repository.ModelRepositoryImpl
import com.riox432.civitdeck.data.repository.ReviewRepositoryImpl
import com.riox432.civitdeck.data.repository.TagRepositoryImpl
import com.riox432.civitdeck.data.repository.UpdateRepositoryImpl
import com.riox432.civitdeck.domain.repository.AuthRepository
import com.riox432.civitdeck.domain.repository.CreatorFollowRepository
import com.riox432.civitdeck.domain.repository.ModelDirectoryRepository
import com.riox432.civitdeck.domain.repository.ModelFileHashRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.ModelScanRepository
import com.riox432.civitdeck.domain.repository.ReviewRepository
import com.riox432.civitdeck.domain.repository.TagRepository
import com.riox432.civitdeck.domain.repository.UpdateRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin module for repositories that combine network ([core-network]) with local
 * cache/storage ([core-database]). Kept separate from [databaseModule] so that
 * core-database stays a pure local-storage layer with no network dependency.
 *
 * Must be loaded after both networkModule and databaseModule so the network
 * APIs and local DAOs/data sources these repositories depend on are available.
 */
val coreDataModule = module {
    single<ModelRepository> { ModelRepositoryImpl(get(), get(), get()) }
    singleOf(::LocalModelFileRepositoryImpl) {
        bind<ModelDirectoryRepository>()
        bind<ModelScanRepository>()
        bind<ModelFileHashRepository>()
    }
    single<CreatorFollowRepository> { CreatorFollowRepositoryImpl(get(), get(), get()) }
    single<UpdateRepository> { UpdateRepositoryImpl(get(), get(), get()) }

    // CivitAI network-only repositories (moved from networkModule for organizational consistency)
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<TagRepository> { TagRepositoryImpl(get()) }
    single<ReviewRepository> { ReviewRepositoryImpl(get(), get()) }
}
