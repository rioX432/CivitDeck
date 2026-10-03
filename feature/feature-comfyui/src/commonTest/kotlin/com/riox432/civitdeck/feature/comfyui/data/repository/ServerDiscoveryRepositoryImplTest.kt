package com.riox432.civitdeck.feature.comfyui.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val HOST_COUNT = 254

/**
 * Covers [ServerDiscoveryRepositoryImpl]: a successful LAN scan surfaces the host
 * answering on port 8188, the scan finishes within the probe-timeout bound when every
 * other host hangs, the no-subnet path emits only the initial empty list, and
 * a fully-unreachable subnet yields no discovered servers.
 */
class ServerDiscoveryRepositoryImplTest {

    private class FakeLocalIpProvider(private val subnet: String?) : LocalIpProvider {
        override fun getLocalSubnet(): String? = subnet
    }

    @Test
    fun scanForServers_discovers_host_answering_on_port_8188() = runTest {
        // Only 10.0.0.5 responds OK; every other probe fails (connection refused analogue).
        val client = mockClient { req ->
            if (req.url.host == "10.0.0.5") okJson("""{"queue_running":[],"queue_pending":[]}""")
            else respondError(HttpStatusCode.NotFound)
        }
        val repo = ServerDiscoveryRepositoryImpl(client, testJson, FakeLocalIpProvider("10.0.0"))

        val emissions = repo.scanForServers().toList()

        val found = emissions.last()
        assertEquals(1, found.size)
        assertEquals("10.0.0.5", found.first().ip)
        assertEquals(8188, found.first().port)
    }

    @Test
    fun scanForServers_emits_only_empty_list_when_subnet_unknown() = runTest {
        val client = mockClient { error("must not probe when subnet is null") }
        val repo = ServerDiscoveryRepositoryImpl(client, testJson, FakeLocalIpProvider(null))

        val emissions = repo.scanForServers().toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions.single().isEmpty())
    }

    @Test
    fun scanForServers_completes_in_bounded_time_when_all_other_hosts_hang() = runTest {
        // The engine runs on the test scheduler so probe timeouts elapse in virtual time.
        val engine = MockEngine.create {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { req ->
                if (req.url.host == "192.168.10.12") okJson("""{"queue_running":[],"queue_pending":[]}""")
                else awaitCancellation()
            }
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(testJson) } }
        val repo = ServerDiscoveryRepositoryImpl(client, testJson, FakeLocalIpProvider("192.168.10"))

        val emissions = repo.scanForServers().toList()

        assertEquals(listOf("192.168.10.12"), emissions.last().map { it.ip })
        val rounds = (HOST_COUNT + PROBE_CONCURRENCY - 1) / PROBE_CONCURRENCY
        assertTrue(
            testScheduler.currentTime <= rounds * PROBE_TIMEOUT_MS,
            "scan took ${testScheduler.currentTime}ms of virtual time, bound is ${rounds * PROBE_TIMEOUT_MS}ms",
        )
    }

    @Test
    fun scanForServers_finds_nothing_when_no_host_responds() = runTest {
        val client = mockClient { respondError(HttpStatusCode.NotFound) }
        val repo = ServerDiscoveryRepositoryImpl(client, testJson, FakeLocalIpProvider("192.168.1"))

        val emissions = repo.scanForServers().toList()

        // Only the initial empty emission; no servers discovered.
        assertTrue(emissions.all { it.isEmpty() })
    }
}
