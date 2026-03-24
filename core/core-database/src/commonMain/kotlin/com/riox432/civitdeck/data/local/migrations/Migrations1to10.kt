package com.riox432.civitdeck.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

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
