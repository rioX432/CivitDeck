package com.riox432.civitdeck.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `plugins` (" +
                "`id` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`version` TEXT NOT NULL, " +
                "`author` TEXT NOT NULL, " +
                "`description` TEXT NOT NULL, " +
                "`pluginType` TEXT NOT NULL, " +
                "`capabilities` TEXT NOT NULL, " +
                "`minAppVersion` TEXT NOT NULL, " +
                "`state` TEXT NOT NULL, " +
                "`configJson` TEXT NOT NULL DEFAULT '{}', " +
                "`installedAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
    }
}

val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN feedQualityThreshold INTEGER NOT NULL DEFAULT 30",
        )
    }
}

val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE feed_cache ADD COLUMN downloadCount INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE feed_cache ADD COLUMN favoriteCount INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE feed_cache ADD COLUMN commentCount INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE feed_cache ADD COLUMN ratingCount INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE feed_cache ADD COLUMN rating REAL NOT NULL DEFAULT 0.0")
    }
}

val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `share_hashtags` (" +
                "`tag` TEXT NOT NULL, " +
                "`isEnabled` INTEGER NOT NULL DEFAULT 1, " +
                "`isCustom` INTEGER NOT NULL DEFAULT 0, " +
                "`addedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`tag`))",
        )
    }
}

val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN autoUpdateCheckEnabled INTEGER NOT NULL DEFAULT 1",
        )
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN lastUpdateCheckTimestamp INTEGER NOT NULL DEFAULT 0",
        )
    }
}

val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE browsing_history ADD COLUMN modelName TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE browsing_history ADD COLUMN thumbnailUrl TEXT")
    }
}

val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE browsing_history ADD COLUMN durationMs INTEGER")
        connection.execSQL("ALTER TABLE browsing_history ADD COLUMN interactionType TEXT")
    }
}

val MIGRATION_37_38 = object : Migration(37, 38) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `model_update_notifications` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`modelId` INTEGER NOT NULL, " +
                "`modelName` TEXT NOT NULL, " +
                "`newVersionName` TEXT NOT NULL, " +
                "`newVersionId` INTEGER NOT NULL, " +
                "`source` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`isRead` INTEGER NOT NULL DEFAULT 0)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_model_update_notifications_createdAt` " +
                "ON `model_update_notifications` (`createdAt`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_model_update_notifications_isRead` " +
                "ON `model_update_notifications` (`isRead`)",
        )
    }
}
