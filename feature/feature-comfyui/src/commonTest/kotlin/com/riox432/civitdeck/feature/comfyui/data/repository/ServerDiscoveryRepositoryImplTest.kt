package com.riox432.civitdeck.feature.comfyui.data.repository

import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [ServerDiscoveryRepositoryImpl]: a successful LAN scan surfaces the host
 * answering on port 8188, the no-subnet path emits only the initial empty list, and
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
    fun scanForServers_finds_nothing_when_no_host_responds() = runTest {
        val client = mockClient { respondError(HttpStatusCode.NotFound) }
        val repo = ServerDiscoveryRepositoryImpl(client, testJson, FakeLocalIpProvider("192.168.1"))

        val emissions = repo.scanForServers().toList()

        // Only the initial empty emission; no servers discovered.
        assertTrue(emissions.all { it.isEmpty() })
    }
}
