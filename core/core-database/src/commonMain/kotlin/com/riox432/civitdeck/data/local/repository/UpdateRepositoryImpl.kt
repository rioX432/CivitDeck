package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.api.GitHubAsset
import com.riox432.civitdeck.data.api.GitHubReleaseApi
import com.riox432.civitdeck.data.local.dao.UserPreferencesDao
import com.riox432.civitdeck.data.local.entity.UserPreferencesEntity
import com.riox432.civitdeck.domain.model.UpdateResult
import com.riox432.civitdeck.domain.repository.AppVersionProvider
import com.riox432.civitdeck.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UpdateRepositoryImpl(
    private val gitHubReleaseApi: GitHubReleaseApi,
    private val appVersionProvider: AppVersionProvider,
    private val preferencesDao: UserPreferencesDao,
) : UpdateRepository {

    override suspend fun checkForUpdate(): UpdateResult {
        val release = gitHubReleaseApi.getLatestRelease()
        val currentVersion = appVersionProvider.getVersionName()
        val latestVersion = release.tagName.removePrefix("v")

        val isNewer = compareVersions(latestVersion, currentVersion) > 0
        val downloadUrl = selectDownloadUrl(release.assets)

        return UpdateResult(
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            releaseNotes = release.body ?: "",
            downloadUrl = downloadUrl,
            htmlUrl = release.htmlUrl,
            isUpdateAvailable = isNewer,
        )
    }

    override fun observeAutoUpdateCheckEnabled(): Flow<Boolean> =
        preferencesDao.observePreferences().map { it?.autoUpdateCheckEnabled ?: true }

    override suspend fun setAutoUpdateCheckEnabled(enabled: Boolean) {
        val existing = preferencesDao.getPreferences() ?: UserPreferencesEntity()
        preferencesDao.upsert(existing.copy(autoUpdateCheckEnabled = enabled))
    }

    override fun observeLastUpdateCheckTimestamp(): Flow<Long> =
        preferencesDao.observePreferences().map { it?.lastUpdateCheckTimestamp ?: 0L }

    override suspend fun setLastUpdateCheckTimestamp(timestamp: Long) {
        val existing = preferencesDao.getPreferences() ?: UserPreferencesEntity()
        preferencesDao.upsert(existing.copy(lastUpdateCheckTimestamp = timestamp))
    }

    private fun selectDownloadUrl(assets: List<GitHubAsset>): String {
        return assets.firstOrNull { it.name.endsWith(".apk") }?.browserDownloadUrl
            ?: assets.firstOrNull()?.browserDownloadUrl
            ?: ""
    }

    companion object {
        fun compareVersions(a: String, b: String): Int {
            val aParts = a.split(".").map { it.toIntOrNull() ?: 0 }
            val bParts = b.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(aParts.size, bParts.size)) {
                val aVal = aParts.getOrElse(i) { 0 }
                val bVal = bParts.getOrElse(i) { 0 }
                if (aVal != bVal) return aVal.compareTo(bVal)
            }
            return 0
        }
    }
}
