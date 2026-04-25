package com.riox432.civitdeck.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.riox432.civitdeck.data.local.dao.BrowsingHistoryDao
import com.riox432.civitdeck.data.local.dao.CachedApiResponseDao
import com.riox432.civitdeck.data.local.dao.CollectionDao
import com.riox432.civitdeck.data.local.dao.ComfyUIConnectionDao
import com.riox432.civitdeck.data.local.dao.DatasetCollectionDao
import com.riox432.civitdeck.data.local.dao.DatasetImageMetaDao
import com.riox432.civitdeck.data.local.dao.ExcludedTagDao
import com.riox432.civitdeck.data.local.dao.ExternalServerConfigDao
import com.riox432.civitdeck.data.local.dao.FeedCacheDao
import com.riox432.civitdeck.data.local.dao.FollowedCreatorDao
import com.riox432.civitdeck.data.local.dao.HiddenModelDao
import com.riox432.civitdeck.data.local.dao.LocalModelFileDao
import com.riox432.civitdeck.data.local.dao.ModelDownloadDao
import com.riox432.civitdeck.data.local.dao.ModelEmbeddingDao
import com.riox432.civitdeck.data.local.dao.ModelNoteDao
import com.riox432.civitdeck.data.local.dao.ModelUpdateNotificationDao
import com.riox432.civitdeck.data.local.dao.ModelVersionCheckpointDao
import com.riox432.civitdeck.data.local.dao.PersonalTagDao
import com.riox432.civitdeck.data.local.dao.PluginDao
import com.riox432.civitdeck.data.local.dao.QualityScoreCacheDao
import com.riox432.civitdeck.data.local.dao.SDWebUIConnectionDao
import com.riox432.civitdeck.data.local.dao.SavedPromptDao
import com.riox432.civitdeck.data.local.dao.SavedSearchFilterDao
import com.riox432.civitdeck.data.local.dao.SearchHistoryDao
import com.riox432.civitdeck.data.local.dao.ShareHashtagDao
import com.riox432.civitdeck.data.local.dao.UserPreferencesDao
import com.riox432.civitdeck.data.local.entity.BrowsingHistoryEntity
import com.riox432.civitdeck.data.local.entity.CachedApiResponseEntity
import com.riox432.civitdeck.data.local.entity.CaptionEntity
import com.riox432.civitdeck.data.local.entity.CollectionEntity
import com.riox432.civitdeck.data.local.entity.CollectionModelEntity
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.data.local.entity.DatasetCollectionEntity
import com.riox432.civitdeck.data.local.entity.DatasetImageEntity
import com.riox432.civitdeck.data.local.entity.ExcludedTagEntity
import com.riox432.civitdeck.data.local.entity.ExternalServerConfigEntity
import com.riox432.civitdeck.data.local.entity.FeedCacheEntity
import com.riox432.civitdeck.data.local.entity.FollowedCreatorEntity
import com.riox432.civitdeck.data.local.entity.HiddenModelEntity
import com.riox432.civitdeck.data.local.entity.ImageTagEntity
import com.riox432.civitdeck.data.local.entity.LocalModelFileEntity
import com.riox432.civitdeck.data.local.entity.ModelDirectoryEntity
import com.riox432.civitdeck.data.local.entity.ModelDownloadEntity
import com.riox432.civitdeck.data.local.entity.ModelEmbeddingEntity
import com.riox432.civitdeck.data.local.entity.ModelNoteEntity
import com.riox432.civitdeck.data.local.entity.ModelUpdateNotificationEntity
import com.riox432.civitdeck.data.local.entity.ModelVersionCheckpointEntity
import com.riox432.civitdeck.data.local.entity.PersonalTagEntity
import com.riox432.civitdeck.data.local.entity.PluginEntity
import com.riox432.civitdeck.data.local.entity.QualityScoreCacheEntity
import com.riox432.civitdeck.data.local.entity.SDWebUIConnectionEntity
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import com.riox432.civitdeck.data.local.entity.SavedSearchFilterEntity
import com.riox432.civitdeck.data.local.entity.SearchHistoryEntity
import com.riox432.civitdeck.data.local.entity.ShareHashtagEntity
import com.riox432.civitdeck.data.local.entity.UserPreferencesEntity
import com.riox432.civitdeck.data.local.migrations.MIGRATION_10_11
import com.riox432.civitdeck.data.local.migrations.MIGRATION_11_12
import com.riox432.civitdeck.data.local.migrations.MIGRATION_12_13
import com.riox432.civitdeck.data.local.migrations.MIGRATION_13_14
import com.riox432.civitdeck.data.local.migrations.MIGRATION_14_15
import com.riox432.civitdeck.data.local.migrations.MIGRATION_15_16
import com.riox432.civitdeck.data.local.migrations.MIGRATION_16_17
import com.riox432.civitdeck.data.local.migrations.MIGRATION_17_18
import com.riox432.civitdeck.data.local.migrations.MIGRATION_18_19
import com.riox432.civitdeck.data.local.migrations.MIGRATION_19_20
import com.riox432.civitdeck.data.local.migrations.MIGRATION_1_2
import com.riox432.civitdeck.data.local.migrations.MIGRATION_20_21
import com.riox432.civitdeck.data.local.migrations.MIGRATION_21_22
import com.riox432.civitdeck.data.local.migrations.MIGRATION_22_23
import com.riox432.civitdeck.data.local.migrations.MIGRATION_23_24
import com.riox432.civitdeck.data.local.migrations.MIGRATION_24_25
import com.riox432.civitdeck.data.local.migrations.MIGRATION_25_26
import com.riox432.civitdeck.data.local.migrations.MIGRATION_26_27
import com.riox432.civitdeck.data.local.migrations.MIGRATION_27_28
import com.riox432.civitdeck.data.local.migrations.MIGRATION_28_29
import com.riox432.civitdeck.data.local.migrations.MIGRATION_29_30
import com.riox432.civitdeck.data.local.migrations.MIGRATION_2_3
import com.riox432.civitdeck.data.local.migrations.MIGRATION_30_31
import com.riox432.civitdeck.data.local.migrations.MIGRATION_31_32
import com.riox432.civitdeck.data.local.migrations.MIGRATION_32_33
import com.riox432.civitdeck.data.local.migrations.MIGRATION_33_34
import com.riox432.civitdeck.data.local.migrations.MIGRATION_34_35
import com.riox432.civitdeck.data.local.migrations.MIGRATION_35_36
import com.riox432.civitdeck.data.local.migrations.MIGRATION_36_37
import com.riox432.civitdeck.data.local.migrations.MIGRATION_37_38
import com.riox432.civitdeck.data.local.migrations.MIGRATION_38_39
import com.riox432.civitdeck.data.local.migrations.MIGRATION_39_40
import com.riox432.civitdeck.data.local.migrations.MIGRATION_3_4
import com.riox432.civitdeck.data.local.migrations.MIGRATION_40_41
import com.riox432.civitdeck.data.local.migrations.MIGRATION_41_42
import com.riox432.civitdeck.data.local.migrations.MIGRATION_42_43
import com.riox432.civitdeck.data.local.migrations.MIGRATION_43_44
import com.riox432.civitdeck.data.local.migrations.MIGRATION_4_5
import com.riox432.civitdeck.data.local.migrations.MIGRATION_5_6
import com.riox432.civitdeck.data.local.migrations.MIGRATION_6_7
import com.riox432.civitdeck.data.local.migrations.MIGRATION_7_8
import com.riox432.civitdeck.data.local.migrations.MIGRATION_8_9
import com.riox432.civitdeck.data.local.migrations.MIGRATION_9_10
import com.riox432.civitdeck.data.local.migrations.defaultCollectionCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        CollectionEntity::class,
        CollectionModelEntity::class,
        CachedApiResponseEntity::class,
        UserPreferencesEntity::class,
        SavedPromptEntity::class,
        SearchHistoryEntity::class,
        BrowsingHistoryEntity::class,
        ExcludedTagEntity::class,
        HiddenModelEntity::class,
        ModelDirectoryEntity::class,
        LocalModelFileEntity::class,
        ModelVersionCheckpointEntity::class,
        ComfyUIConnectionEntity::class,
        SDWebUIConnectionEntity::class,
        DatasetCollectionEntity::class,
        DatasetImageEntity::class,
        ImageTagEntity::class,
        CaptionEntity::class,
        SavedSearchFilterEntity::class,
        ExternalServerConfigEntity::class,
        ModelNoteEntity::class,
        PersonalTagEntity::class,
        FollowedCreatorEntity::class,
        FeedCacheEntity::class,
        ModelDownloadEntity::class,
        PluginEntity::class,
        ShareHashtagEntity::class,
        ModelUpdateNotificationEntity::class,
        QualityScoreCacheEntity::class,
        ModelEmbeddingEntity::class,
    ],
    version = 44,
)
@ConstructedBy(CivitDeckDatabaseConstructor::class)
abstract class CivitDeckDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun cachedApiResponseDao(): CachedApiResponseDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun savedPromptDao(): SavedPromptDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun browsingHistoryDao(): BrowsingHistoryDao
    abstract fun excludedTagDao(): ExcludedTagDao
    abstract fun hiddenModelDao(): HiddenModelDao
    abstract fun localModelFileDao(): LocalModelFileDao
    abstract fun modelVersionCheckpointDao(): ModelVersionCheckpointDao
    abstract fun comfyUIConnectionDao(): ComfyUIConnectionDao
    abstract fun sdWebUIConnectionDao(): SDWebUIConnectionDao
    abstract fun datasetCollectionDao(): DatasetCollectionDao
    abstract fun datasetImageMetaDao(): DatasetImageMetaDao
    abstract fun savedSearchFilterDao(): SavedSearchFilterDao
    abstract fun externalServerConfigDao(): ExternalServerConfigDao
    abstract fun modelNoteDao(): ModelNoteDao
    abstract fun personalTagDao(): PersonalTagDao
    abstract fun followedCreatorDao(): FollowedCreatorDao
    abstract fun feedCacheDao(): FeedCacheDao
    abstract fun modelDownloadDao(): ModelDownloadDao
    abstract fun pluginDao(): PluginDao
    abstract fun shareHashtagDao(): ShareHashtagDao
    abstract fun modelUpdateNotificationDao(): ModelUpdateNotificationDao
    abstract fun qualityScoreCacheDao(): QualityScoreCacheDao
    abstract fun modelEmbeddingDao(): ModelEmbeddingDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object CivitDeckDatabaseConstructor : RoomDatabaseConstructor<CivitDeckDatabase>

fun getRoomDatabase(builder: RoomDatabase.Builder<CivitDeckDatabase>): CivitDeckDatabase {
    return builder
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16,
            MIGRATION_16_17,
            MIGRATION_17_18,
            MIGRATION_18_19,
            MIGRATION_19_20,
            MIGRATION_20_21,
            MIGRATION_21_22,
            MIGRATION_22_23,
            MIGRATION_23_24,
            MIGRATION_24_25,
            MIGRATION_25_26,
            MIGRATION_26_27,
            MIGRATION_27_28,
            MIGRATION_28_29,
            MIGRATION_29_30,
            MIGRATION_30_31,
            MIGRATION_31_32,
            MIGRATION_32_33,
            MIGRATION_33_34,
            MIGRATION_34_35,
            MIGRATION_35_36,
            MIGRATION_36_37,
            MIGRATION_37_38,
            MIGRATION_38_39,
            MIGRATION_39_40,
            MIGRATION_40_41,
            MIGRATION_41_42,
            MIGRATION_42_43,
            MIGRATION_43_44,
        )
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .addCallback(defaultCollectionCallback)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

internal const val DB_FILE_NAME = "civitdeck.db"
