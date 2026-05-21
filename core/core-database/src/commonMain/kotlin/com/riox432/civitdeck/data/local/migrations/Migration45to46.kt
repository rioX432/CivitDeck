package com.riox432.civitdeck.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_45_46 = object : Migration(45, 46) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE comfyui_connections ADD COLUMN ntfyServerUrl TEXT DEFAULT NULL",
        )
        connection.execSQL(
            "ALTER TABLE comfyui_connections ADD COLUMN ntfyTopic TEXT DEFAULT NULL",
        )
    }
}
