package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.GitHubReleaseApi
import com.riox432.civitdeck.data.local.dao.UserPreferencesDao
import com.riox432.civitdeck.data.local.entity.UserPreferencesEntity
import com.riox432.civitdeck.domain.repository.AppVersionProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [UpdateRepositoryImpl]'s version comparison logic and the auto-update /
 * last-check preference accessors (backed by a fake [UserPreferencesDao]).
 *
 * Note: [UpdateRepositoryImpl.checkForUpdate] is not unit-tested here because it
 * depends on [GitHubReleaseApi], which is a final class that constructs its own
 * Ktor [io.ktor.client.HttpClient] internally with no injectable engine — it can
 * only be exercised against the real GitHub network.
 */
class UpdateRepositoryImplTest {

    private class FakeUserPreferencesDao : UserPreferencesDao {
        val state = MutableStateFlow<UserPreferencesEntity?>(null)

        override fun observePreferences(): Flow<UserPreferencesEntity?> = state

        override suspend fun getPreferences(): UserPreferencesEntity? = state.value

        override suspend fun upsert(entity: UserPreferencesEntity) {
            state.value = entity
        }
    }

    private class FakeAppVersionProvider(private val version: String) : AppVersionProvider {
        override fun getVersionName(): String = version
    }

    private fun repo(
        dao: UserPreferencesDao = FakeUserPreferencesDao(),
        version: String = "1.0.0",
    ): UpdateRepositoryImpl = UpdateRepositoryImpl(
        gitHubReleaseApi = GitHubReleaseApi(Json { ignoreUnknownKeys = true }),
        appVersionProvider = FakeAppVersionProvider(version),
        preferencesDao = dao,
    )

    @Test
    fun compareVersions_orders_by_numeric_segments() {
        assertTrue(UpdateRepositoryImpl.compareVersions("2.0.0", "1.9.9") > 0)
        assertTrue(UpdateRepositoryImpl.compareVersions("1.2.0", "1.10.0") < 0)
        assertEquals(0, UpdateRepositoryImpl.compareVersions("1.2.3", "1.2.3"))
    }

    @Test
    fun compareVersions_treats_missing_segments_as_zero() {
        assertEquals(0, UpdateRepositoryImpl.compareVersions("1.2", "1.2.0"))
        assertTrue(UpdateRepositoryImpl.compareVersions("1.2.1", "1.2") > 0)
    }

    @Test
    fun compareVersions_treats_non_numeric_segments_as_zero() {
        // "1.x.0" -> [1, 0, 0]; equal to "1.0.0".
        assertEquals(0, UpdateRepositoryImpl.compareVersions("1.x.0", "1.0.0"))
    }

    @Test
    fun observeAutoUpdateCheckEnabled_defaults_to_true_when_no_preferences() = runTest {
        val result = repo().observeAutoUpdateCheckEnabled().first()
        assertTrue(result)
    }

    @Test
    fun setAutoUpdateCheckEnabled_persists_and_is_observable() = runTest {
        val dao = FakeUserPreferencesDao()
        val repo = repo(dao)

        repo.setAutoUpdateCheckEnabled(false)

        assertEquals(false, dao.state.value?.autoUpdateCheckEnabled)
        assertEquals(false, repo.observeAutoUpdateCheckEnabled().first())
    }

    @Test
    fun setAutoUpdateCheckEnabled_preserves_other_preference_fields() = runTest {
        val dao = FakeUserPreferencesDao()
        dao.state.value = UserPreferencesEntity(apiKey = "secret", gridColumns = 4)
        val repo = repo(dao)

        repo.setAutoUpdateCheckEnabled(false)

        assertEquals("secret", dao.state.value?.apiKey)
        assertEquals(4, dao.state.value?.gridColumns)
        assertEquals(false, dao.state.value?.autoUpdateCheckEnabled)
    }

    @Test
    fun observeLastUpdateCheckTimestamp_defaults_to_zero_when_no_preferences() = runTest {
        assertEquals(0L, repo().observeLastUpdateCheckTimestamp().first())
    }

    @Test
    fun setLastUpdateCheckTimestamp_persists_value() = runTest {
        val dao = FakeUserPreferencesDao()
        val repo = repo(dao)

        repo.setLastUpdateCheckTimestamp(123_456L)

        assertEquals(123_456L, dao.state.value?.lastUpdateCheckTimestamp)
        assertEquals(123_456L, repo.observeLastUpdateCheckTimestamp().first())
    }
}
