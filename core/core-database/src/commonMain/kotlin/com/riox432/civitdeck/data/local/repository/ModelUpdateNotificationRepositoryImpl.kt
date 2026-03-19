package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.ModelUpdateNotificationDao
import com.riox432.civitdeck.data.local.entity.ModelUpdateNotificationEntity
import com.riox432.civitdeck.domain.model.ModelUpdate
import com.riox432.civitdeck.domain.model.ModelUpdateNotification
import com.riox432.civitdeck.domain.model.UpdateSource
import com.riox432.civitdeck.domain.repository.ModelUpdateNotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ModelUpdateNotificationRepositoryImpl(
    private val dao: ModelUpdateNotificationDao,
) : ModelUpdateNotificationRepository {

    override fun observeNotifications(): Flow<List<ModelUpdateNotification>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeUnreadCount(): Flow<Int> = dao.observeUnreadCount()

    override suspend fun saveNotifications(
        updates: List<ModelUpdate>,
        source: UpdateSource,
    ) {
        if (updates.isEmpty()) return
        val now = currentTimeMillis()
        val entities = updates.map { update ->
            ModelUpdateNotificationEntity(
                modelId = update.modelId,
                modelName = update.modelName,
                newVersionName = update.newVersionName,
                newVersionId = update.newVersionId,
                source = source.name,
                createdAt = now,
            )
        }
        dao.insertAll(entities)
    }

    override suspend fun markRead(notificationId: Long) {
        dao.markRead(notificationId)
    }

    override suspend fun markAllRead() {
        dao.markAllRead()
    }

    override suspend fun cleanupOldNotifications() {
        val threshold = currentTimeMillis() - RETENTION_MS
        dao.deleteOlderThan(threshold)
    }

    private fun ModelUpdateNotificationEntity.toDomain() = ModelUpdateNotification(
        id = id,
        modelId = modelId,
        modelName = modelName,
        newVersionName = newVersionName,
        newVersionId = newVersionId,
        source = try {
            UpdateSource.valueOf(source)
        } catch (_: IllegalArgumentException) {
            UpdateSource.FAVORITE
        },
        createdAt = createdAt,
        isRead = isRead,
    )

    companion object {
        private const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }
}
