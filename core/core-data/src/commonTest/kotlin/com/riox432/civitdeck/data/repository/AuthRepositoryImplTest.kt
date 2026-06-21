package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [AuthRepositoryImpl.validateApiKey]: returns the username on success and
 * a failed [Result] (not a thrown exception) when the network call fails.
 */
class AuthRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun apiReturning(body: String): CivitAiApi {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return CivitAiApi(HttpClient(engine) { install(ContentNegotiation) { json(json) } })
    }

    private fun offlineApi(): CivitAiApi {
        val engine = MockEngine { throw IOException("offline") }
        return CivitAiApi(HttpClient(engine) { install(ContentNegotiation) { json(json) } })
    }

    @Test
    fun validateApiKey_returns_username_on_success() = runTest {
        val body = """{"id":1,"username":"bob","image":null}"""
        val repo = AuthRepositoryImpl(apiReturning(body))

        val result = repo.validateApiKey("valid-key")

        assertTrue(result.isSuccess)
        assertEquals("bob", result.getOrNull())
    }

    @Test
    fun validateApiKey_sends_bearer_authorization_header() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel("""{"id":1,"username":"bob"}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = CivitAiApi(HttpClient(engine) { install(ContentNegotiation) { json(json) } })
        val repo = AuthRepositoryImpl(api)

        repo.validateApiKey("my-secret")

        val authHeader = engine.requestHistory.first().headers[HttpHeaders.Authorization]
        assertEquals("Bearer my-secret", authHeader)
    }

    @Test
    fun validateApiKey_returns_failure_when_network_fails() = runTest {
        val repo = AuthRepositoryImpl(offlineApi())

        val result = repo.validateApiKey("any-key")

        assertTrue(result.isFailure)
    }
}
