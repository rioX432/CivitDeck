package com.riox432.civitdeck.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.riox432.civitdeck.data.local.dao.BrowsingHistoryDao
import com.riox432.civitdeck.data.local.dao.CachedApiResponseDao
import com.riox432.civitdeck.data.local.dao.ExcludedTagDao
import com.riox432.civitdeck.data.local.dao.FavoriteModelDao
import com.riox432.civitdeck.data.local.dao.HiddenModelDao
import com.riox432.civitdeck.data.local.dao.SavedPromptDao
import com.riox432.civitdeck.data.local.dao.SearchHistoryDao
import com.riox432.civitdeck.data.local.dao.UserPreferencesDao
import com.riox432.civitdeck.data.local.entity.BrowsingHistoryEntity
import com.riox432.civitdeck.data.local.entity.CachedApiResponseEntity
import com.riox432.civitdeck.data.local.entity.ExcludedTagEntity
import com.riox432.civitdeck.data.local.entity.FavoriteModelEntity
import com.riox432.civitdeck.data.local.entity.HiddenModelEntity
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import com.riox432.civitdeck.data.local.entity.SearchHistoryEntity
import com.riox432.civitdeck.data.local.entity.UserPreferencesEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        FavoriteModelEntity::class,
        CachedApiResponseEntity::class,
        UserPreferencesEntity::class,
        SavedPromptEntity::class,
        SearchHistoryEntity::class,
        BrowsingHistoryEntity::class,
        ExcludedTagEntity::class,
        HiddenModelEntity::class,
    ],
    version = 6,
)
@ConstructedBy(CivitDeckDatabaseConstructor::class)
abstract class CivitDeckDatabase : RoomDatabase() {
    abstract fun favoriteModelDao(): FavoriteModelDao
    abstract fun cachedApiResponseDao(): CachedApiResponseDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun savedPromptDao(): SavedPromptDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun browsingHistoryDao(): BrowsingHistoryDao
    abstract fun excludedTagDao(): ExcludedTagDao
    abstract fun hiddenModelDao(): HiddenModelDao
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
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_prompts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                prompt TEXT NOT NULL,
                negativePrompt TEXT,
                sampler TEXT,
                steps INTEGER,
                cfgScale REAL,
                seed INTEGER,
                modelName TEXT,
                size TEXT,
                sourceImageUrl TEXT,
                savedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `search_history` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`query` TEXT NOT NULL, `searchedAt` INTEGER NOT NULL)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `browsing_history` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`modelId` INTEGER NOT NULL, " +
                "`modelType` TEXT NOT NULL, " +
                "`creatorName` TEXT, " +
                "`tags` TEXT NOT NULL, " +
                "`viewedAt` INTEGER NOT NULL)",
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `excluded_tags` " +
                "(`tag` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`tag`))",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `hidden_models` " +
                "(`modelId` INTEGER NOT NULL, `modelName` TEXT NOT NULL, " +
                "`hiddenAt` INTEGER NOT NULL, PRIMARY KEY(`modelId`))",
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_browsing_history_modelId` " +
                "ON `browsing_history` (`modelId`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_browsing_history_viewedAt` " +
                "ON `browsing_history` (`viewedAt`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cached_api_responses_cachedAt` " +
                "ON `cached_api_responses` (`cachedAt`)",
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN defaultSortOrder TEXT NOT NULL DEFAULT 'MostDownloaded'",
        )
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN defaultTimePeriod TEXT NOT NULL DEFAULT 'AllTime'",
        )
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN gridColumns INTEGER NOT NULL DEFAULT 2",
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE user_preferences ADD COLUMN apiKey TEXT DEFAULT NULL",
        )
    }
}

fun getRoomDatabase(builder: RoomDatabase.Builder<CivitDeckDatabase>): CivitDeckDatabase {
    return builder
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

internal const val DB_FILE_NAME = "civitdeck.db"
