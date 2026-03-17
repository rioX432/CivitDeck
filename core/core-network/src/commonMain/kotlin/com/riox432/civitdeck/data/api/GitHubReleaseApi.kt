package com.riox432.civitdeck.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    val size: Long = 0,
)

class GitHubReleaseApi(json: Json) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun getLatestRelease(): GitHubRelease {
        return client.get(LATEST_RELEASE_URL) {
            header("Accept", "application/vnd.github+json")
        }.body()
    }

    companion object {
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/rioX432/CivitDeck/releases/latest"
    }
}
