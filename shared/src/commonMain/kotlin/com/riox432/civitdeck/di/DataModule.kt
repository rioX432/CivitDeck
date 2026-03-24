package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.backup.BackupRepositoryImpl
import com.riox432.civitdeck.data.backup.CollectionDaos
import com.riox432.civitdeck.data.backup.ConnectionDaos
import com.riox432.civitdeck.data.backup.ContentDaos
import com.riox432.civitdeck.data.backup.PreferenceDaos
import com.riox432.civitdeck.data.export.ExportRepositoryImpl
import com.riox432.civitdeck.data.export.KohyaZipExportPlugin
import com.riox432.civitdeck.data.image.ImageSaver
import com.riox432.civitdeck.data.repository.AuthRepositoryImpl
import com.riox432.civitdeck.data.repository.BrowsingHistoryRepositoryImpl
import com.riox432.civitdeck.data.repository.CacheRepositoryImpl
import com.riox432.civitdeck.data.repository.CreatorFollowRepositoryImpl
import com.riox432.civitdeck.data.repository.FavoriteRepositoryImpl
import com.riox432.civitdeck.data.repository.LocalModelFileRepositoryImpl
import com.riox432.civitdeck.data.repository.ModelRepositoryImpl
import com.riox432.civitdeck.data.repository.ModelVersionCheckpointRepositoryImpl
import com.riox432.civitdeck.data.repository.ReviewRepositoryImpl
import com.riox432.civitdeck.data.repository.TagRepositoryImpl
import com.riox432.civitdeck.data.repository.UpdateRepositoryImpl
import com.riox432.civitdeck.data.scanner.FileScanner
import com.riox432.civitdeck.domain.repository.AuthRepository
import com.riox432.civitdeck.domain.repository.BackupRepository
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import com.riox432.civitdeck.domain.repository.CacheRepository
import com.riox432.civitdeck.domain.repository.CreatorFollowRepository
import com.riox432.civitdeck.domain.repository.ExportRepository
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.ModelDirectoryRepository
import com.riox432.civitdeck.domain.repository.ModelFileHashRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.ModelScanRepository
import com.riox432.civitdeck.domain.repository.ModelVersionCheckpointRepository
import com.riox432.civitdeck.domain.repository.ReviewRepository
import com.riox432.civitdeck.domain.repository.TagRepository
import com.riox432.civitdeck.domain.repository.UpdateRepository
import com.riox432.civitdeck.usecase.ActivateThemePluginUseCase
import com.riox432.civitdeck.usecase.ExportWithPluginUseCase
import com.riox432.civitdeck.usecase.GetActiveThemeUseCase
import com.riox432.civitdeck.usecase.GetAvailableExportFormatsUseCase
import com.riox432.civitdeck.usecase.ImportThemeUseCase
import com.riox432.civitdeck.usecase.ObserveThemePluginsUseCase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {
    // File Scanner
    factory { FileScanner() }

    // Image Saver (platform-specific via expect/actual)
    factory { ImageSaver() }

    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<CacheRepository> { CacheRepositoryImpl(get()) }
    single<ModelRepository> { ModelRepositoryImpl(get(), get(), get()) }
    single<TagRepository> { TagRepositoryImpl(get()) }
    single<FavoriteRepository> { FavoriteRepositoryImpl(get()) }
    single<BrowsingHistoryRepository> { BrowsingHistoryRepositoryImpl(get()) }
    singleOf(::LocalModelFileRepositoryImpl) {
        bind<ModelDirectoryRepository>()
        bind<ModelScanRepository>()
        bind<ModelFileHashRepository>()
    }
    single<ModelVersionCheckpointRepository> { ModelVersionCheckpointRepositoryImpl(get()) }
    single<ReviewRepository> { ReviewRepositoryImpl(get(), get()) }
    single<CreatorFollowRepository> { CreatorFollowRepositoryImpl(get(), get(), get()) }
    single<UpdateRepository> { UpdateRepositoryImpl(get(), get(), get()) }

    // Export
    single<ExportRepository> { ExportRepositoryImpl(get(), get()) }
    single { KohyaZipExportPlugin(get()) }
    factory { GetAvailableExportFormatsUseCase(get()) }
    factory { ExportWithPluginUseCase(get()) }

    // Theme plugins
    factory { ImportThemeUseCase(get(), get()) }
    factory { GetActiveThemeUseCase(get()) }
    factory { ObserveThemePluginsUseCase(get()) }
    factory { ActivateThemePluginUseCase(get(), get()) }

    // Backup DAO wrappers
    single { CollectionDaos(get()) }
    single { ConnectionDaos(get(), get(), get()) }
    single { ContentDaos(get(), get(), get(), get(), get()) }
    single { PreferenceDaos(get(), get(), get()) }
    single<BackupRepository> { BackupRepositoryImpl(get(), get(), get(), get()) }
}
