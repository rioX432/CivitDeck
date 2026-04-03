package com.riox432.civitdeck.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Suppress("MatchingDeclarationName")
val MIGRATION_39_40 = object : Migration(39, 40) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE model_downloads ADD COLUMN expectedSha256 TEXT DEFAULT NULL",
        )
        connection.execSQL(
            "ALTER TABLE model_downloads ADD COLUMN hashVerified INTEGER DEFAULT NULL",
        )
    }
}
