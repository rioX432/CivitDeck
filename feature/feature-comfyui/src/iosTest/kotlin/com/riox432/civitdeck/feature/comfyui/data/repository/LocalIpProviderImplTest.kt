package com.riox432.civitdeck.feature.comfyui.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalIpProviderImplTest {

    @Test
    fun privateAddressesYieldTheirSlash24() {
        assertEquals("192.168.10", privateSubnetOrNull(listOf(192, 168, 10, 23)))
        assertEquals("192.168.0", privateSubnetOrNull(listOf(192, 168, 0, 5)))
        assertEquals("10.0.0", privateSubnetOrNull(listOf(10, 0, 0, 42)))
        assertEquals("172.16.4", privateSubnetOrNull(listOf(172, 16, 4, 1)))
        assertEquals("172.31.255", privateSubnetOrNull(listOf(172, 31, 255, 254)))
    }

    @Test
    fun nonPrivateAddressesYieldNull() {
        assertNull(privateSubnetOrNull(listOf(172, 15, 0, 1)))
        assertNull(privateSubnetOrNull(listOf(172, 32, 0, 1)))
        assertNull(privateSubnetOrNull(listOf(169, 254, 3, 7)))
        assertNull(privateSubnetOrNull(listOf(100, 64, 0, 1)))
        assertNull(privateSubnetOrNull(listOf(8, 8, 8, 8)))
        assertNull(privateSubnetOrNull(listOf(192, 169, 1, 1)))
    }

    @Test
    fun malformedOctetListYieldsNull() {
        assertNull(privateSubnetOrNull(emptyList()))
        assertNull(privateSubnetOrNull(listOf(192, 168, 1)))
    }

    @Test
    fun getLocalSubnetReturnsNullOrAPrivateSlash24() {
        // The host's en0 varies by machine, so only the shape of the result is checked.
        val subnet = LocalIpProviderImpl().getLocalSubnet() ?: return
        val octets = subnet.split('.').map { it.toInt() }
        assertEquals(subnet, privateSubnetOrNull(octets + 1))
    }
}
