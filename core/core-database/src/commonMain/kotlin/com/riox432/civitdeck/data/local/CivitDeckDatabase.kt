package com.riox432.civitdeck.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.riox432.civitdeck.data.local.dao.BrowsingHistoryDao
import com.riox432.civitdeck.data.local.dao.CachedApiResponseDao
import com.riox432.civitdeck.data.local.dao.CollectionDao
import com.riox432.civitdeck.data.local.dao.ComfyUIConnectionDao
import com.riox432.civitdeck.data.local.dao.DatasetCollectionDao
import com.riox432.civitdeck.data.local.dao.DatasetImageMetaDao
import com.riox432.civitdeck.data.local.dao.ExcludedTagDao
import com.riox432.civitdeck.data.local.dao.HiddenModelDao
import com.riox432.civitdeck.data.local.dao.LocalModelFileDao
import com.riox432.civitdeck.data.local.dao.ModelVersionCheckpointDao
import com.riox432.civitdeck.data.local.dao.SDWebUIConnectionDao
import com.riox432.civitdeck.data.local.dao.SavedPromptDao
import com.riox432.civitdeck.data.local.dao.SearchHistoryDao
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
import com.riox432.civitdeck.data.local.entity.HiddenModelEntity
import com.riox432.civitdeck.data.local.entity.ImageTagEntity
import com.riox432.civitdeck.data.local.entity.LocalModelFileEntity
import com.riox432.civitdeck.data.local.entity.ModelDirectoryEntity
import com.riox432.civitdeck.data.local.entity.ModelVersionCheckpointEntity
import com.riox432.civitdeck.data.local.entity.SDWebUIConnectionEntity
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import com.riox432.civitdeck.data.local.entity.SearchHistoryEntity
import com.riox432.civitdeck.data.local.entity.UserPreferencesEntity
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
    ],
    version = 24,
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
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object CivitDeckDatabaseConstructor : RoomDatabaseConstructor<CivitDeckDatabase>

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `user_preferences` " +
                "(`id` INTEGER NOT NULL DEFAULT 1, `nsfwFilterLevel` TEXT NOT NULL DEFAULT 'Off', " +
                "PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_prompts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                prompt TEXT NOT NULL,
                negativePrompt TEXT,
                sampler TEXT,
                steps INTEGER,
                cfgScale REAL,
                seed INTEGER,
                modelName TEXT,
                size TEXT,
                sourceImageUrl TEXT,
                savedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `search_history` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`query` TEXT NOT NULL, `searchedAt` INTEGER NOT NULL)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `browsing_history` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`modelId` INTEGER NOT NULL, " +
                "`modelType` TEXT NOT NULL, " +
                "`creatorName` TEXT, " +
                "`tags` TEXT NOT NULL, " +
                "`viewedAt` INTEGER NOT NULL)",
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `excluded_tags` " +
                "(`tag` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`tag`))",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `hidden_models` " +
                "(`modelId` INTEGER NOT NULL, `modelName` TEXT NOT NULL, " +
                "`hiddenAt` INTEGER NOT NULL, PRIMARY KEY(`modelId`))",
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_browsing_history_modelId` " +
                "ON `browsing_history` (`modelId`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_browsing_history_viewedAt` " +
                "ON `browsing_history` (`viewedAt`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cached_api_responses_cachedAt` " +
                "ON `cached_api_responses` (`cachedAt`)",
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN defaultSortOrder TEXT NOT NULL DEFAULT 'MostDownloaded'",
        )
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN defaultTimePeriod TEXT NOT NULL DEFAULT 'AllTime'",
        )
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN gridColumns INTEGER NOT NULL DEFAULT 2",
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN apiKey TEXT DEFAULT NULL",
        )
    }
}

@Suppress("LongMethod")
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        // Create collections table
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `collections` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`isDefault` INTEGER NOT NULL DEFAULT 0, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL)",
        )
        // Insert default "Favorites" collection with id=1
        connection.execSQL(
            "INSERT INTO `collections` (`id`, `name`, `isDefault`, `createdAt`, `updatedAt`) " +
                "VALUES (1, 'Favorites', 1, 0, 0)",
        )
        // Create collection_model_entries table
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `collection_model_entries` (" +
                "`collectionId` INTEGER NOT NULL, " +
                "`modelId` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`nsfw` INTEGER NOT NULL, " +
                "`thumbnailUrl` TEXT, " +
                "`creatorName` TEXT, " +
                "`downloadCount` INTEGER NOT NULL, " +
                "`favoriteCount` INTEGER NOT NULL, " +
                "`rating` REAL NOT NULL, " +
                "`addedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`collectionId`, `modelId`), " +
                "FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON DELETE CASCADE)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_collection_model_entries_modelId` " +
                "ON `collection_model_entries` (`modelId`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_collection_model_entries_collectionId` " +
                "ON `collection_model_entries` (`collectionId`)",
        )
        // Migrate existing favorites into default collection
        connection.execSQL(
            "INSERT INTO `collection_model_entries` " +
                "(`collectionId`, `modelId`, `name`, `type`, `nsfw`, `thumbnailUrl`, " +
                "`creatorName`, `downloadCount`, `favoriteCount`, `rating`, `addedAt`) " +
                "SELECT 1, `id`, `name`, `type`, `nsfw`, `thumbnailUrl`, " +
                "`creatorName`, `downloadCount`, `favoriteCount`, `rating`, `favoritedAt` " +
                "FROM `favorite_models`",
        )
        // Drop old table
        connection.execSQL("DROP TABLE IF EXISTS `favorite_models`")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN powerUserMode INTEGER NOT NULL DEFAULT 0",
        )
    }
}

@Suppress("LongMethod")
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `model_directories` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`path` TEXT NOT NULL, " +
                "`label` TEXT, " +
                "`lastScannedAt` INTEGER, " +
                "`isEnabled` INTEGER NOT NULL DEFAULT 1)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `local_model_files` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`directoryId` INTEGER NOT NULL, " +
                "`filePath` TEXT NOT NULL, " +
                "`fileName` TEXT NOT NULL, " +
                "`sha256Hash` TEXT NOT NULL, " +
                "`sizeBytes` INTEGER NOT NULL, " +
                "`scannedAt` INTEGER NOT NULL, " +
                "`matchedModelId` INTEGER, " +
                "`matchedModelName` TEXT, " +
                "`matchedVersionId` INTEGER, " +
                "`matchedVersionName` TEXT, " +
                "`latestVersionId` INTEGER, " +
                "`hasUpdate` INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(`directoryId`) REFERENCES `model_directories`(`id`) ON DELETE CASCADE)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_local_model_files_directoryId` " +
                "ON `local_model_files` (`directoryId`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_local_model_files_sha256Hash` " +
                "ON `local_model_files` (`sha256Hash`)",
        )
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE saved_prompts ADD COLUMN isTemplate INTEGER NOT NULL DEFAULT 0",
        )
        connection.execSQL(
            "ALTER TABLE saved_prompts ADD COLUMN templateName TEXT",
        )
        connection.execSQL(
            "ALTER TABLE saved_prompts ADD COLUMN category TEXT",
        )
        connection.execSQL(
            "ALTER TABLE saved_prompts ADD COLUMN autoSaved INTEGER NOT NULL DEFAULT 0",
        )
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 0",
        )
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN pollingIntervalMinutes INTEGER NOT NULL DEFAULT 0",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `model_version_checkpoints` (" +
                "`modelId` INTEGER NOT NULL, " +
                "`lastKnownVersionId` INTEGER NOT NULL, " +
                "`lastCheckedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`modelId`))",
        )
    }
}

// No-op migration: category column remains in DB but is no longer mapped to entity
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        // category column stays in the table; Room ignores unmapped columns
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN nsfwBlurSoft INTEGER NOT NULL DEFAULT 75",
        )
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN nsfwBlurMature INTEGER NOT NULL DEFAULT 25",
        )
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN nsfwBlurExplicit INTEGER NOT NULL DEFAULT 0",
        )
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(connection: SQLiteConnection) {
        // Add offline pinning support to cached_api_responses
        connection.execSQL(
            "ALTER TABLE cached_api_responses ADD COLUMN isOfflinePinned INTEGER NOT NULL DEFAULT 0",
        )
        // Add offline cache settings to user_preferences
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN offlineCacheEnabled INTEGER NOT NULL DEFAULT 1",
        )
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN cacheSizeLimitMb INTEGER NOT NULL DEFAULT 200",
        )
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN accentColor TEXT NOT NULL DEFAULT 'Blue'",
        )
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN amoledDarkMode INTEGER NOT NULL DEFAULT 0",
        )
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN seenTutorialVersion INTEGER NOT NULL DEFAULT 0",
        )
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `comfyui_connections` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`hostname` TEXT NOT NULL, " +
                "`port` INTEGER NOT NULL DEFAULT 8188, " +
                "`isActive` INTEGER NOT NULL DEFAULT 0, " +
                "`lastTestedAt` INTEGER, " +
                "`lastTestSuccess` INTEGER, " +
                "`createdAt` INTEGER NOT NULL)",
        )
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE saved_prompts DROP COLUMN category")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE saved_prompts ADD COLUMN templateVariables TEXT",
        )
        connection.execSQL(
            "ALTER TABLE saved_prompts ADD COLUMN templateType TEXT",
        )
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `sdwebui_connections` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`hostname` TEXT NOT NULL, " +
                "`port` INTEGER NOT NULL DEFAULT 7860, " +
                "`isActive` INTEGER NOT NULL DEFAULT 0, " +
                "`lastTestedAt` INTEGER, " +
                "`lastTestSuccess` INTEGER, " +
                "`createdAt` INTEGER NOT NULL)",
        )
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN civitaiLinkKey TEXT",
        )
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'SYSTEM'",
        )
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN customNavShortcuts TEXT NOT NULL DEFAULT ''",
        )
    }
}

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `dataset_collections` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`description` TEXT NOT NULL DEFAULT '', " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `dataset_images` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`datasetId` INTEGER NOT NULL, " +
                "`imageUrl` TEXT NOT NULL, " +
                "`sourceType` TEXT NOT NULL, " +
                "`trainable` INTEGER NOT NULL DEFAULT 1, " +
                "`addedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`datasetId`) REFERENCES `dataset_collections`(`id`) ON DELETE CASCADE)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_dataset_images_datasetId` ON `dataset_images`(`datasetId`)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `image_tags` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`datasetImageId` INTEGER NOT NULL, " +
                "`tag` TEXT NOT NULL, " +
                "FOREIGN KEY(`datasetImageId`) REFERENCES `dataset_images`(`id`) ON DELETE CASCADE)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_image_tags_datasetImageId` ON `image_tags`(`datasetImageId`)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `captions` " +
                "(`datasetImageId` INTEGER NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "PRIMARY KEY(`datasetImageId`), " +
                "FOREIGN KEY(`datasetImageId`) REFERENCES `dataset_images`(`id`) ON DELETE CASCADE)",
        )
    }
}

// Seed data is inserted via onOpen (not in migrations) because Room migrations only run on
// upgrades — a fresh install starts directly at the latest schema version, skipping all
// migration callbacks. Using INSERT OR IGNORE in onOpen ensures required rows are always
// present on every app launch, whether on a new install or after an upgrade.
private val defaultCollectionCallback = object : RoomDatabase.Callback() {
    override fun onOpen(connection: SQLiteConnection) {
        super.onOpen(connection)
        connection.execSQL(
            "INSERT OR IGNORE INTO `collections` (`id`, `name`, `isDefault`, `createdAt`, `updatedAt`) " +
                "VALUES (1, 'Favorites', 1, 0, 0)",
        )
        seedBuiltInTemplates(connection)
    }
}

@Suppress("LongMethod")
private fun seedBuiltInTemplates(connection: SQLiteConnection) {
    // INSERT OR IGNORE guarantees idempotency: rows with the same negative IDs are silently
    // skipped if they already exist, so this function is safe to call on every app open.
    // id=-1 → TXT2IMG built-in template
    connection.execSQL(
        """INSERT OR IGNORE INTO saved_prompts
            (id, prompt, negativePrompt, sampler, steps, cfgScale, seed, modelName, size,
             sourceImageUrl, savedAt, isTemplate, templateName, autoSaved, templateVariables, templateType)
           VALUES (-1, '{positive_prompt}', '{negative_prompt}', 'euler', 20, 7.0, -1,
                   '{checkpoint}', '{width}x{height}', NULL, 0, 1, 'txt2img Default', 0,
                   '[{"name":"positive_prompt","type":"TEXT","defaultValue":"","options":[],"required":true},{"name":"negative_prompt","type":"TEXT","defaultValue":"","options":[],"required":false},{"name":"checkpoint","type":"TEXT","defaultValue":"","options":[],"required":true},{"name":"steps","type":"NUMBER","defaultValue":"20","options":[],"required":false},{"name":"cfg","type":"NUMBER","defaultValue":"7.0","options":[],"required":false},{"name":"width","type":"NUMBER","defaultValue":"512","options":[],"required":false},{"name":"height","type":"NUMBER","defaultValue":"512","options":[],"required":false}]',
                   'TXT2IMG')""",
    )
    // id=-2 → IMG2IMG built-in template
    connection.execSQL(
        """INSERT OR IGNORE INTO saved_prompts
            (id, prompt, negativePrompt, sampler, steps, cfgScale, seed, modelName, size,
             sourceImageUrl, savedAt, isTemplate, templateName, autoSaved, templateVariables, templateType)
           VALUES (-2, '{positive_prompt}', '{negative_prompt}', 'euler', 20, 7.0, -1,
                   '{checkpoint}', '{width}x{height}', NULL, 0, 1, 'img2img Default', 0,
                   '[{"name":"positive_prompt","type":"TEXT","defaultValue":"","options":[],"required":true},{"name":"negative_prompt","type":"TEXT","defaultValue":"","options":[],"required":false},{"name":"checkpoint","type":"TEXT","defaultValue":"","options":[],"required":true},{"name":"steps","type":"NUMBER","defaultValue":"20","options":[],"required":false},{"name":"cfg","type":"NUMBER","defaultValue":"7.0","options":[],"required":false},{"name":"width","type":"NUMBER","defaultValue":"512","options":[],"required":false},{"name":"height","type":"NUMBER","defaultValue":"512","options":[],"required":false},{"name":"denoise_strength","type":"NUMBER","defaultValue":"0.75","options":[],"required":false}]',
                   'IMG2IMG')""",
    )
    // id=-3 → UPSCALE built-in template
    connection.execSQL(
        """INSERT OR IGNORE INTO saved_prompts
            (id, prompt, negativePrompt, sampler, steps, cfgScale, seed, modelName, size,
             sourceImageUrl, savedAt, isTemplate, templateName, autoSaved, templateVariables, templateType)
           VALUES (-3, '{input_image}', NULL, 'euler', 20, 7.0, -1,
                   NULL, '512x512', NULL, 0, 1, 'Upscale Default', 0,
                   '[{"name":"input_image","type":"TEXT","defaultValue":"","options":[],"required":true},{"name":"upscale_factor","type":"NUMBER","defaultValue":"2","options":[],"required":false}]',
                   'UPSCALE')""",
    )
}

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
        )
        .addCallback(defaultCollectionCallback)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

internal const val DB_FILE_NAME = "civitdeck.db"
