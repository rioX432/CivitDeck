package com.riox432.civitdeck.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_49_50 = object : Migration(49, 50) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE comfyui_connections ADD COLUMN tlsCertSha256 TEXT",
        )
    }
}
