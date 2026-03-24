package com.riox432.civitdeck.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Suppress("MatchingDeclarationName")
val MIGRATION_38_39 = object : Migration(38, 39) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE saved_search_filters ADD COLUMN selectedSources TEXT NOT NULL DEFAULT 'CIVITAI'",
        )
    }
}
