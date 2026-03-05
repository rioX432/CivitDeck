package com.riox432.civitdeck.domain.model

data class ExternalServerConfig(
    val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val apiKey: String = "",
    val isActive: Boolean = false,
    val lastTestedAt: Long? = null,
    val lastTestSuccess: Boolean? = null,
    val createdAt: Long,
) {
    val normalizedBaseUrl: String
        get() = baseUrl.trimEnd('/')
}

enum class ExternalServerConnectionStatus {
    Connected,
    Disconnected,
    Testing,
    Error,
    NotConfigured,
}
