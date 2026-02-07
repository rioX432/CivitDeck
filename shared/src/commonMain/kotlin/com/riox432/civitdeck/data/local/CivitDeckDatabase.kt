package com.riox432.civitdeck.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.riox432.civitdeck.data.local.dao.CachedApiResponseDao
import com.riox432.civitdeck.data.local.dao.FavoriteModelDao
import com.riox432.civitdeck.data.local.dao.UserPreferencesDao
import com.riox432.civitdeck.data.local.entity.CachedApiResponseEntity
import com.riox432.civitdeck.data.local.entity.FavoriteModelEntity
import com.riox432.civitdeck.data.local.entity.UserPreferencesEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        FavoriteModelEntity::class,
        CachedApiResponseEntity::class,
        UserPreferencesEntity::class,
    ],
    version = 2,
)
@ConstructedBy(CivitDeckDatabaseConstructor::class)
abstract class CivitDeckDatabase : RoomDatabase() {
    abstract fun favoriteModelDao(): FavoriteModelDao
    abstract fun cachedApiResponseDao(): CachedApiResponseDao
    abstract fun userPreferencesDao(): UserPreferencesDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object CivitDeckDatabaseConstructor : RoomDatabaseConstructor<CivitDeckDatabase>

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `user_preferences` " +
                "(`id` INTEGER NOT NULL DEFAULT 1, `nsfwFilterLevel` TEXT NOT NULL DEFAULT 'Off', " +
                "PRIMARY KEY(`id`))",
        )
    }
}

fun getRoomDatabase(builder: RoomDatabase.Builder<CivitDeckDatabase>): CivitDeckDatabase {
    return builder
        .addMigrations(MIGRATION_1_2)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

internal const val DB_FILE_NAME = "civitdeck.db"
