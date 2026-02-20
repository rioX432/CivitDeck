package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.data.local.dao.UserPreferencesDao
import com.riox432.civitdeck.data.local.entity.UserPreferencesEntity
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserPreferencesRepositoryImplTest {

    private class FakeDao : UserPreferencesDao {
        val store = MutableStateFlow<UserPreferencesEntity?>(null)

        override fun observePreferences(): Flow<UserPreferencesEntity?> = store

        override suspend fun getPreferences(): UserPreferencesEntity? = store.value

        override suspend fun upsert(entity: UserPreferencesEntity) {
            store.value = entity
        }
    }

    private fun createRepo(
        dao: FakeDao = FakeDao(),
        apiKeyProvider: ApiKeyProvider = ApiKeyProvider(),
    ) = Triple(dao, apiKeyProvider, UserPreferencesRepositoryImpl(dao, apiKeyProvider))

    // --- NSFW Filter Level ---

    @Test
    fun observeNsfwFilterLevel_defaults_to_Off() = runTest {
        val (_, _, repo) = createRepo()
        assertEquals(NsfwFilterLevel.Off, repo.observeNsfwFilterLevel().first())
    }

    @Test
    fun setNsfwFilterLevel_persists_and_emits() = runTest {
        val (dao, _, repo) = createRepo()
        repo.setNsfwFilterLevel(NsfwFilterLevel.All)
        assertEquals("All", dao.store.value?.nsfwFilterLevel)
        assertEquals(NsfwFilterLevel.All, repo.observeNsfwFilterLevel().first())
    }

    // --- Sort Order ---

    @Test
    fun observeDefaultSortOrder_defaults_to_MostDownloaded() = runTest {
        val (_, _, repo) = createRepo()
        assertEquals(SortOrder.MostDownloaded, repo.observeDefaultSortOrder().first())
    }

    @Test
    fun setDefaultSortOrder_persists() = runTest {
        val (_, _, repo) = createRepo()
        repo.setDefaultSortOrder(SortOrder.Newest)
        assertEquals(SortOrder.Newest, repo.observeDefaultSortOrder().first())
    }

    // --- Time Period ---

    @Test
    fun observeDefaultTimePeriod_defaults_to_AllTime() = runTest {
        val (_, _, repo) = createRepo()
        assertEquals(TimePeriod.AllTime, repo.observeDefaultTimePeriod().first())
    }

    @Test
    fun setDefaultTimePeriod_persists() = runTest {
        val (_, _, repo) = createRepo()
        repo.setDefaultTimePeriod(TimePeriod.Month)
        assertEquals(TimePeriod.Month, repo.observeDefaultTimePeriod().first())
    }

    // --- Grid Columns ---

    @Test
    fun observeGridColumns_defaults_to_2() = runTest {
        val (_, _, repo) = createRepo()
        assertEquals(2, repo.observeGridColumns().first())
    }

    @Test
    fun setGridColumns_persists() = runTest {
        val (_, _, repo) = createRepo()
        repo.setGridColumns(3)
        assertEquals(3, repo.observeGridColumns().first())
    }

    // --- API Key ---

    @Test
    fun observeApiKey_defaults_to_null() = runTest {
        val (_, _, repo) = createRepo()
        assertNull(repo.observeApiKey().first())
    }

    @Test
    fun setApiKey_persists_and_syncs_to_provider() = runTest {
        val (_, apiKeyProvider, repo) = createRepo()
        repo.setApiKey("test-key")
        assertEquals("test-key", repo.observeApiKey().first())
        assertEquals("test-key", repo.getApiKey())
        assertEquals("test-key", apiKeyProvider.apiKey)
    }

    @Test
    fun setApiKey_null_clears_both_dao_and_provider() = runTest {
        val (_, apiKeyProvider, repo) = createRepo()
        repo.setApiKey("test-key")
        repo.setApiKey(null)
        assertNull(repo.observeApiKey().first())
        assertNull(apiKeyProvider.apiKey)
    }

    // --- Multiple preferences preserved ---

    @Test
    fun setting_one_preference_preserves_others() = runTest {
        val (_, _, repo) = createRepo()
        repo.setNsfwFilterLevel(NsfwFilterLevel.All)
        repo.setGridColumns(3)
        // nsfw should still be All after setting gridColumns
        assertEquals(NsfwFilterLevel.All, repo.observeNsfwFilterLevel().first())
        assertEquals(3, repo.observeGridColumns().first())
    }
}
