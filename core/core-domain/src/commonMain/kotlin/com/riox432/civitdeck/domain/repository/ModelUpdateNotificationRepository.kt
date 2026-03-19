package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ModelUpdate
import com.riox432.civitdeck.domain.model.ModelUpdateNotification
import com.riox432.civitdeck.domain.model.UpdateSource
import kotlinx.coroutines.flow.Flow

interface ModelUpdateNotificationRepository {
    fun observeNotifications(): Flow<List<ModelUpdateNotification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun saveNotifications(updates: List<ModelUpdate>, source: UpdateSource)
    suspend fun markRead(notificationId: Long)
    suspend fun markAllRead()
    suspend fun cleanupOldNotifications()
}
