package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.civitailink.CivitaiLinkApi
import com.riox432.civitdeck.data.api.civitailink.CivitaiLinkOutgoingMessage
import com.riox432.civitdeck.data.api.civitailink.CivitaiLinkResourceItem
import com.riox432.civitdeck.domain.model.CivitaiLinkActivity
import com.riox432.civitdeck.domain.model.CivitaiLinkResource
import com.riox432.civitdeck.domain.model.CivitaiLinkStatus
import com.riox432.civitdeck.domain.repository.CivitaiLinkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.io.Closeable
import com.riox432.civitdeck.data.api.civitailink.CivitaiLinkResource as ApiResource

class CivitaiLinkRepositoryImpl(
    private val api: CivitaiLinkApi,
) : CivitaiLinkRepository, Closeable {

    private val _status = MutableStateFlow(CivitaiLinkStatus.Disconnected)
    private val _activities = MutableStateFlow<List<CivitaiLinkActivity>>(emptyList())
    private var connectionJob: Job? = null
    private var activeKey: String? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun observeStatus(): Flow<CivitaiLinkStatus> = _status.asStateFlow()
    override fun observeActivities(): Flow<List<CivitaiLinkActivity>> = _activities.asStateFlow()
    override fun isConnected(): Boolean = _status.value == CivitaiLinkStatus.Connected

    override suspend fun connect(key: String) {
        if (activeKey == key && _status.value == CivitaiLinkStatus.Connected) return
        disconnect()
        activeKey = key
        _status.value = CivitaiLinkStatus.Connecting
        connectionJob = scope.launch {
            try {
                api.observeLink(key).collect { msg ->
                    when (msg.type) {
                        "activities" -> {
                            _status.value = CivitaiLinkStatus.Connected
                            _activities.value = msg.payload?.activities?.map { it.toDomain() }
                                ?: emptyList()
                        }
                        "error" -> _status.value = CivitaiLinkStatus.Error
                        else -> _status.value = CivitaiLinkStatus.Connected
                    }
                }
                _status.value = CivitaiLinkStatus.Disconnected
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                println("CivitaiLinkRepositoryImpl: Connection failed: ${e.message}")
                _status.value = CivitaiLinkStatus.Error
            }
        }
    }

    override fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        activeKey = null
        _status.value = CivitaiLinkStatus.Disconnected
        _activities.value = emptyList()
    }

    override fun close() {
        scope.cancel()
    }

    override suspend fun sendResourceToPC(resource: CivitaiLinkResource) {
        val key = activeKey ?: return
        val command = CivitaiLinkOutgoingMessage(
            command = "resources:add",
            payload = ApiResource(
                resource = CivitaiLinkResourceItem(
                    id = resource.versionId,
                    modelId = resource.modelId,
                    name = resource.versionName,
                    downloadUrl = resource.downloadUrl,
                )
            )
        )
        api.sendCommand(key, command)
    }

    override suspend fun cancelActivity(activityId: String) {
        val key = activeKey ?: return
        api.sendCommand(
            key,
            CivitaiLinkOutgoingMessage(
                command = "activities:cancel",
                payload = null,
            )
        )
    }

    private fun com.riox432.civitdeck.data.api.civitailink.CivitaiLinkActivity.toDomain() =
        CivitaiLinkActivity(
            id = id,
            type = type,
            status = status,
            progress = progress,
            error = error,
        )
}
