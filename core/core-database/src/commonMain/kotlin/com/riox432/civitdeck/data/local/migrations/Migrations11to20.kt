package com.riox432.civitdeck.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

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
