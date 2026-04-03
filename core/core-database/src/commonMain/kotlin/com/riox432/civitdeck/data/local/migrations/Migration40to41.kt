package com.riox432.civitdeck.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Suppress("MatchingDeclarationName")
val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE saved_prompts ADD COLUMN templateMetadata TEXT DEFAULT NULL",
        )
    }
}
