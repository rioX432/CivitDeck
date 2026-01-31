package com.omooooori.civitdeck.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<CivitDeckDatabase> {
    val dbFile = context.getDatabasePath(DB_FILE_NAME)
    return Room.databaseBuilder<CivitDeckDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
}
