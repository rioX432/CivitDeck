package com.riox432.civitdeck.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_46_47 = object : Migration(46, 47) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE saved_prompts ADD COLUMN isAppMode INTEGER NOT NULL DEFAULT 0",
        )
        connection.execSQL(
            "ALTER TABLE saved_prompts ADD COLUMN rawWorkflowJson TEXT DEFAULT NULL",
        )
    }
}
