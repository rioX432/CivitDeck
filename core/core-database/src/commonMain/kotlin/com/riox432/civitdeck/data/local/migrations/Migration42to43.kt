package com.riox432.civitdeck.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_42_43 = object : Migration(42, 43) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS model_embeddings (
                modelId INTEGER NOT NULL PRIMARY KEY,
                embeddingModel TEXT NOT NULL,
                dim INTEGER NOT NULL,
                embedding BLOB NOT NULL,
                cachedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}
