package com.riox432.civitdeck.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_41_42 = object : Migration(41, 42) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS quality_score_cache (
                modelId INTEGER NOT NULL PRIMARY KEY,
                score INTEGER NOT NULL,
                downloadCount INTEGER NOT NULL,
                favoriteCount INTEGER NOT NULL,
                ratingCount INTEGER NOT NULL,
                cachedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}
