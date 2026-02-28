package com.riox432.civitdeck.domain.model

enum class CivitaiLinkStatus {
    Disconnected,
    Connecting,
    Connected,
    Error,
}

data class CivitaiLinkActivity(
    val id: String,
    val type: String,
    val status: String,
    val progress: Double,
    val error: String? = null,
)

data class CivitaiLinkResource(
    val versionId: Long,
    val modelId: Long,
    val versionName: String,
    val downloadUrl: String,
)
