package com.riox432.civitdeck.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

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

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE dataset_images ADD COLUMN licenseNote TEXT")
        connection.execSQL("ALTER TABLE dataset_images ADD COLUMN pHash TEXT")
        connection.execSQL("ALTER TABLE dataset_images ADD COLUMN excluded INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE dataset_images ADD COLUMN width INTEGER")
        connection.execSQL("ALTER TABLE dataset_images ADD COLUMN height INTEGER")
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `saved_search_filters` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`query` TEXT NOT NULL DEFAULT '', " +
                "`selectedType` TEXT, " +
                "`selectedSort` TEXT NOT NULL DEFAULT 'MostDownloaded', " +
                "`selectedPeriod` TEXT NOT NULL DEFAULT 'AllTime', " +
                "`selectedBaseModels` TEXT NOT NULL DEFAULT '', " +
                "`nsfwFilterLevel` TEXT NOT NULL DEFAULT 'Off', " +
                "`isFreshFindEnabled` INTEGER NOT NULL DEFAULT 0, " +
                "`excludedTags` TEXT NOT NULL DEFAULT '', " +
                "`includedTags` TEXT NOT NULL DEFAULT '', " +
                "`savedAt` INTEGER NOT NULL)",
        )
    }
}

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `external_server_configs` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`baseUrl` TEXT NOT NULL, " +
                "`apiKey` TEXT NOT NULL DEFAULT '', " +
                "`isActive` INTEGER NOT NULL DEFAULT 0, " +
                "`lastTestedAt` INTEGER, " +
                "`lastTestSuccess` INTEGER, " +
                "`createdAt` INTEGER NOT NULL)",
        )
    }
}

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `model_notes` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`modelId` INTEGER NOT NULL, " +
                "`noteText` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_model_notes_modelId` ON `model_notes` (`modelId`)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `personal_tags` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`modelId` INTEGER NOT NULL, " +
                "`tag` TEXT NOT NULL, " +
                "`addedAt` INTEGER NOT NULL)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_personal_tags_modelId` ON `personal_tags` (`modelId`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_personal_tags_tag` ON `personal_tags` (`tag`)",
        )
    }
}

val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `followed_creators` (" +
                "`username` TEXT NOT NULL, " +
                "`displayName` TEXT NOT NULL, " +
                "`avatarUrl` TEXT, " +
                "`followedAt` INTEGER NOT NULL, " +
                "`lastCheckedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`username`))",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `feed_cache` (" +
                "`modelId` INTEGER NOT NULL, " +
                "`creatorUsername` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`thumbnailUrl` TEXT, " +
                "`type` TEXT NOT NULL, " +
                "`publishedAt` TEXT NOT NULL, " +
                "`cachedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`modelId`))",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_feed_cache_creatorUsername` " +
                "ON `feed_cache` (`creatorUsername`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_feed_cache_publishedAt` " +
                "ON `feed_cache` (`publishedAt`)",
        )
    }
}

val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `model_downloads` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`modelId` INTEGER NOT NULL, " +
                "`modelName` TEXT NOT NULL, " +
                "`versionId` INTEGER NOT NULL, " +
                "`versionName` TEXT NOT NULL, " +
                "`fileId` INTEGER NOT NULL, " +
                "`fileName` TEXT NOT NULL, " +
                "`fileUrl` TEXT NOT NULL, " +
                "`fileSizeBytes` INTEGER NOT NULL, " +
                "`downloadedBytes` INTEGER NOT NULL DEFAULT 0, " +
                "`status` TEXT NOT NULL DEFAULT 'Pending', " +
                "`modelType` TEXT NOT NULL, " +
                "`destinationPath` TEXT, " +
                "`errorMessage` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_model_downloads_modelId` " +
                "ON `model_downloads` (`modelId`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_model_downloads_status` " +
                "ON `model_downloads` (`status`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_model_downloads_createdAt` " +
                "ON `model_downloads` (`createdAt`)",
        )
    }
}
