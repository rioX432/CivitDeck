package com.riox432.civitdeck.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Adds the append-only `interaction_event` table and backfills the single-column
 * interaction signal previously stored on `browsing_history`. The backfilled
 * `timestamp` uses `viewedAt` as an approximation — only the most recent overwritten
 * interaction per row survives, since earlier ones were already lost under the old
 * overwrite scheme.
 */
val MIGRATION_48_49 = object : Migration(48, 49) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `interaction_event` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`modelId` INTEGER NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_interaction_event_timestamp` " +
                "ON `interaction_event` (`timestamp`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_interaction_event_type_timestamp` " +
                "ON `interaction_event` (`type`, `timestamp`)",
        )
        connection.execSQL(
            "INSERT INTO `interaction_event` (`modelId`, `type`, `timestamp`) " +
                "SELECT modelId, interactionType, viewedAt FROM browsing_history " +
                "WHERE interactionType IS NOT NULL",
        )
    }
}
