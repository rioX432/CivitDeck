package com.riox432.civitdeck.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSHomeDirectory

fun getDatabaseBuilder(): RoomDatabase.Builder<CivitDeckDatabase> {
    val dbFilePath = NSHomeDirectory() + "/Documents/$DB_FILE_NAME"
    return Room.databaseBuilder<CivitDeckDatabase>(
        name = dbFilePath,
    )
}
