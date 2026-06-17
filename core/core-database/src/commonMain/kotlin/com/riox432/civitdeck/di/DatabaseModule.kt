package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.backup.BackupRepositoryImpl
import com.riox432.civitdeck.data.backup.CollectionDaos
import com.riox432.civitdeck.data.backup.ConnectionDaos
import com.riox432.civitdeck.data.backup.ContentDaos
import com.riox432.civitdeck.data.backup.PreferenceDaos
import com.riox432.civitdeck.data.local.CivitDeckDatabase
import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.data.local.getRoomDatabase
import com.riox432.civitdeck.data.local.repository.AnalyticsRepositoryImpl
import com.riox432.civitdeck.data.local.repository.BrowsingHistoryRepositoryImpl
import com.riox432.civitdeck.data.local.repository.CacheRepositoryImpl
import com.riox432.civitdeck.data.local.repository.CaptionRepositoryImpl
import com.riox432.civitdeck.data.local.repository.DatasetCollectionRepositoryImpl
import com.riox432.civitdeck.data.local.repository.FavoriteRepositoryImpl
import com.riox432.civitdeck.data.local.repository.ImageTagRepositoryImpl
import com.riox432.civitdeck.data.local.repository.ModelDownloadRepositoryImpl
import com.riox432.civitdeck.data.local.repository.ModelEmbeddingRepositoryImpl
import com.riox432.civitdeck.data.local.repository.ModelNoteRepositoryImpl
import com.riox432.civitdeck.data.local.repository.ModelUpdateNotificationRepositoryImpl
import com.riox432.civitdeck.data.local.repository.ModelVersionCheckpointRepositoryImpl
import com.riox432.civitdeck.data.local.repository.PluginRepositoryImpl
import com.riox432.civitdeck.data.local.repository.ShareHashtagRepositoryImpl
import com.riox432.civitdeck.data.scanner.FileScanner
import com.riox432.civitdeck.domain.repository.AnalyticsRepository
import com.riox432.civitdeck.domain.repository.BackupRepository
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import com.riox432.civitdeck.domain.repository.CacheRepository
import com.riox432.civitdeck.domain.repository.CaptionRepository
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.ImageTagRepository
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository
import com.riox432.civitdeck.domain.repository.ModelEmbeddingRepository
import com.riox432.civitdeck.domain.repository.ModelNoteRepository
import com.riox432.civitdeck.domain.repository.ModelUpdateNotificationRepository
import com.riox432.civitdeck.domain.repository.ModelVersionCheckpointRepository
import com.riox432.civitdeck.domain.repository.PluginRepository
import com.riox432.civitdeck.domain.repository.ShareHashtagRepository
import org.koin.dsl.module

val databaseModule = module {
    // Room Database
    single<CivitDeckDatabase> { getRoomDatabase(get()) }
    single { get<CivitDeckDatabase>().collectionDao() }
    single { get<CivitDeckDatabase>().cachedApiResponseDao() }
    single { get<CivitDeckDatabase>().userPreferencesDao() }
    single { get<CivitDeckDatabase>().savedPromptDao() }
    single { get<CivitDeckDatabase>().searchHistoryDao() }
    single { get<CivitDeckDatabase>().browsingHistoryDao() }
    single { get<CivitDeckDatabase>().excludedTagDao() }
    single { get<CivitDeckDatabase>().hiddenModelDao() }
    single { get<CivitDeckDatabase>().localModelFileDao() }
    single { get<CivitDeckDatabase>().modelVersionCheckpointDao() }
    single { get<CivitDeckDatabase>().comfyUIConnectionDao() }
    single { get<CivitDeckDatabase>().sdWebUIConnectionDao() }
    single { get<CivitDeckDatabase>().datasetCollectionDao() }
    single { get<CivitDeckDatabase>().datasetImageMetaDao() }
    single { get<CivitDeckDatabase>().savedSearchFilterDao() }
    single { get<CivitDeckDatabase>().externalServerConfigDao() }
    single { get<CivitDeckDatabase>().modelNoteDao() }
    single { get<CivitDeckDatabase>().personalTagDao() }
    single { get<CivitDeckDatabase>().followedCreatorDao() }
    single { get<CivitDeckDatabase>().feedCacheDao() }
    single { get<CivitDeckDatabase>().modelDownloadDao() }
    single { get<CivitDeckDatabase>().pluginDao() }
    single { get<CivitDeckDatabase>().shareHashtagDao() }
    single { get<CivitDeckDatabase>().modelUpdateNotificationDao() }
    single { get<CivitDeckDatabase>().qualityScoreCacheDao() }
    single { get<CivitDeckDatabase>().modelEmbeddingDao() }

    // Data Sources
    single { LocalCacheDataSource(get()) }

    // File Scanner
    factory { FileScanner() }

    // Repositories (DB-only)
    single<CacheRepository> { CacheRepositoryImpl(get()) }
    single<FavoriteRepository> { FavoriteRepositoryImpl(get()) }
    single<BrowsingHistoryRepository> { BrowsingHistoryRepositoryImpl(get()) }
    single<ModelVersionCheckpointRepository> { ModelVersionCheckpointRepositoryImpl(get()) }

    // Mixed network + cache repositories (ModelRepository, LocalModelFile*,
    // CreatorFollow, Update) are registered in coreDataModule (core-data).

    // Dataset
    single<DatasetCollectionRepository> { DatasetCollectionRepositoryImpl(get(), get()) }
    single<ImageTagRepository> { ImageTagRepositoryImpl(get()) }
    single<CaptionRepository> { CaptionRepositoryImpl(get()) }

    // Notes & Personal Tags
    single<ModelNoteRepository> { ModelNoteRepositoryImpl(get(), get()) }

    // Analytics
    single<AnalyticsRepository> { AnalyticsRepositoryImpl(get(), get(), get()) }

    // Downloads
    single<ModelDownloadRepository> { ModelDownloadRepositoryImpl(get()) }

    // Plugins
    single<PluginRepository> { PluginRepositoryImpl(get()) }

    // Share Hashtags
    single<ShareHashtagRepository> { ShareHashtagRepositoryImpl(get()) }

    // Model Update Notifications
    single<ModelUpdateNotificationRepository> { ModelUpdateNotificationRepositoryImpl(get()) }

    // Image Embeddings (#699)
    single<ModelEmbeddingRepository> { ModelEmbeddingRepositoryImpl(get()) }

    // Backup DAO wrappers
    single { CollectionDaos(get()) }
    single { ConnectionDaos(get(), get(), get()) }
    single { ContentDaos(get(), get(), get(), get(), get()) }
    single { PreferenceDaos(get(), get(), get()) }
    single<BackupRepository> { BackupRepositoryImpl(get(), get(), get(), get()) }
}
