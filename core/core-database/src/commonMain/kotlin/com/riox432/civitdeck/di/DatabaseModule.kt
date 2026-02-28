package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.local.CivitDeckDatabase
import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.data.local.getRoomDatabase
import org.koin.dsl.module

val databaseModule = module {
    // Room Database
    single<CivitDeckDatabase> { getRoomDatabase(get()) }
    single { get<CivitDeckDatabase>().collectionDao() }
    single { get<CivitDeckDatabase>().cachedApiResponseDao() }
    single { get<CivitDeckDatabase>().userPreferencesDao() }
    single { get<CivitDeckDatabase>().savedPromptDao() }
    single { get<CivitDeckDatabase>().searchHistoryDao() }
    single { get<CivitDeckDatabase>().browsingHistoryDao() }
    single { get<CivitDeckDatabase>().excludedTagDao() }
    single { get<CivitDeckDatabase>().hiddenModelDao() }
    single { get<CivitDeckDatabase>().localModelFileDao() }
    single { get<CivitDeckDatabase>().modelVersionCheckpointDao() }
    single { get<CivitDeckDatabase>().comfyUIConnectionDao() }
    single { get<CivitDeckDatabase>().sdWebUIConnectionDao() }

    // Data Sources
    single { LocalCacheDataSource(get()) }
}
