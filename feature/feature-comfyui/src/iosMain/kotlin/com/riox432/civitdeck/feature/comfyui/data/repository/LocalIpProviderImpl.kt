@file:OptIn(ExperimentalForeignApi::class)

package com.riox432.civitdeck.feature.comfyui.data.repository

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.posix.AF_INET
import platform.posix.sockaddr_in

// Wi-Fi is en0 on iOS devices; cellular interfaces (pdp_ip*) are not a LAN worth scanning.
private const val WIFI_INTERFACE = "en0"
private const val IPV4_OCTETS = 4

class LocalIpProviderImpl : LocalIpProvider {
    override fun getLocalSubnet(): String? = memScoped {
        // Darwin's getifaddrs leaves the head untouched on failure, and freeifaddrs(NULL) is free(NULL).
        val head = alloc<CPointerVar<ifaddrs>> { value = null }
        try {
            if (getifaddrs(head.ptr) != 0) return@memScoped null
            wifiIpv4Octets(head.value)?.let(::privateSubnetOrNull)
        } finally {
            freeifaddrs(head.value)
        }
    }

    private fun wifiIpv4Octets(first: CPointer<ifaddrs>?): List<Int>? {
        var cursor = first
        while (cursor != null) {
            val entry = cursor.pointed
            val addr = entry.ifa_addr
            if (addr != null &&
                addr.pointed.sa_family.toInt() == AF_INET &&
                entry.ifa_name?.toKString() == WIFI_INTERFACE
            ) {
                // sin_addr is in network byte order, so its bytes are already the octets in order.
                val bytes = addr.reinterpret<sockaddr_in>().pointed.sin_addr.ptr.reinterpret<UByteVar>()
                return List(IPV4_OCTETS) { bytes[it].toInt() }
            }
            cursor = entry.ifa_next
        }
        return null
    }
}

/**
 * Returns the first three octets when [octets] is an RFC 1918 address, mirroring
 * `InetAddress.isSiteLocalAddress` on Android/JVM; otherwise null.
 */
@Suppress("MagicNumber")
internal fun privateSubnetOrNull(octets: List<Int>): String? {
    if (octets.size != IPV4_OCTETS) return null
    val (a, b) = octets
    val isPrivate = a == 10 || (a == 172 && b in 16..31) || (a == 192 && b == 168)
    return if (isPrivate) octets.take(3).joinToString(".") else null
}
