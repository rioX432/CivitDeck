package com.riox432.civitdeck.data.api.civitailink

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CivitaiLinkIncomingMessage(
    val type: String,
    val payload: CivitaiLinkPayload? = null,
)

@Serializable
data class CivitaiLinkPayload(
    val activities: List<CivitaiLinkActivity> = emptyList(),
    val message: String? = null,
)

@Serializable
data class CivitaiLinkActivity(
    val id: String,
    val type: String,
    val status: String = "Pending",
    val progress: Double = 0.0,
    val error: String? = null,
)

@Serializable
data class CivitaiLinkOutgoingMessage(
    val type: String = "command",
    val command: String,
    val payload: CivitaiLinkResource? = null,
)

@Serializable
data class CivitaiLinkResource(
    val resource: CivitaiLinkResourceItem,
)

@Serializable
data class CivitaiLinkResourceItem(
    val type: String = "Model Version",
    val id: Long,
    @SerialName("modelId") val modelId: Long,
    val name: String,
    @SerialName("downloadUrl") val downloadUrl: String,
)
