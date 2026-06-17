package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ConnectionFailureCause
import com.riox432.civitdeck.domain.model.ConnectionTestResult
import com.riox432.civitdeck.domain.model.SystemStats
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionTester
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchSystemStatsUseCase
import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.serialization.json.Json

private const val TAG = "ComfyUIConnectionTester"

/**
 * Tests a connection on a transient [ComfyUIApi] wrapping one of the shared, named
 * HttpClients (normal vs self-signed). A fresh [ComfyUIApi] per test avoids the mutable
 * base-URL state of the singleton [ComfyUIApi], so concurrent tests/probes do not race.
 */
class ComfyUIConnectionTesterImpl(
    private val normalClient: HttpClient,
    private val selfSignedClient: HttpClient,
    private val json: Json,
) : ComfyUIConnectionTester {

    override suspend fun test(connection: ComfyUIConnection): ConnectionTestResult {
        val client = if (connection.acceptSelfSigned) selfSignedClient else normalClient
        val api = ComfyUIApi(client, json)
        api.setBaseUrl(connection.baseUrl)
        return try {
            api.getQueue()
            // Health check passed; fetch optional stats (best-effort, never fails the test).
            val stats = fetchStats(api)
            ConnectionTestResult.Success(stats)
        } catch (e: ConnectTimeoutException) {
            failure(connection, ConnectionFailureCause.Timeout, e)
        } catch (e: HttpRequestTimeoutException) {
            failure(connection, ConnectionFailureCause.Timeout, e)
        } catch (e: SocketTimeoutException) {
            failure(connection, ConnectionFailureCause.Timeout, e)
        } catch (e: ResponseException) {
            failure(connection, ConnectionFailureCause.Http, e, e.response.status.value)
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            val cause = if (isTlsFailure(e)) ConnectionFailureCause.Tls else ConnectionFailureCause.Unreachable
            failure(connection, cause, e)
        }
    }

    private suspend fun fetchStats(api: ComfyUIApi): SystemStats? =
        FetchSystemStatsUseCase(api).invoke()

    private fun failure(
        connection: ComfyUIConnection,
        cause: ConnectionFailureCause,
        error: Throwable,
        httpStatus: Int? = null,
    ): ConnectionTestResult.Failure {
        Logger.w(TAG, "Test failed for ${connection.baseUrl}: $cause (${error.message})")
        return ConnectionTestResult.Failure(cause, httpStatus)
    }
}
