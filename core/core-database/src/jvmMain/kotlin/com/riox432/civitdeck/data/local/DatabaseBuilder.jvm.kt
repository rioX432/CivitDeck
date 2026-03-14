package com.riox432.civitdeck.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<CivitDeckDatabase> {
    val dbDir = File(System.getProperty("user.home"), ".civitdeck")
    if (!dbDir.exists()) dbDir.mkdirs()
    val dbFile = File(dbDir, DB_FILE_NAME)
    return Room.databaseBuilder<CivitDeckDatabase>(
        name = dbFile.absolutePath,
    )
}
