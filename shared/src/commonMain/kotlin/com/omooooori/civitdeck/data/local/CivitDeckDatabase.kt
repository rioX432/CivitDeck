package com.omooooori.civitdeck.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.omooooori.civitdeck.data.local.dao.CachedApiResponseDao
import com.omooooori.civitdeck.data.local.dao.FavoriteModelDao
import com.omooooori.civitdeck.data.local.entity.CachedApiResponseEntity
import com.omooooori.civitdeck.data.local.entity.FavoriteModelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [FavoriteModelEntity::class, CachedApiResponseEntity::class],
    version = 1,
)
@ConstructedBy(CivitDeckDatabaseConstructor::class)
abstract class CivitDeckDatabase : RoomDatabase() {
    abstract fun favoriteModelDao(): FavoriteModelDao
    abstract fun cachedApiResponseDao(): CachedApiResponseDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object CivitDeckDatabaseConstructor : RoomDatabaseConstructor<CivitDeckDatabase>

fun getRoomDatabase(builder: RoomDatabase.Builder<CivitDeckDatabase>): CivitDeckDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

internal const val DB_FILE_NAME = "civitdeck.db"
