package com.riox432.civitdeck.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_43_44 = object : Migration(43, 44) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE comfyui_connections ADD COLUMN useHttps INTEGER NOT NULL DEFAULT 0",
        )
        connection.execSQL(
            "ALTER TABLE comfyui_connections ADD COLUMN acceptSelfSigned INTEGER NOT NULL DEFAULT 0",
        )
    }
}
