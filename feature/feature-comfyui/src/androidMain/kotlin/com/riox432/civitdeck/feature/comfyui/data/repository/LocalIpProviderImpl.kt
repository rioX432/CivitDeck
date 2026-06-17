package com.riox432.civitdeck.feature.comfyui.data.repository

import java.net.NetworkInterface

class LocalIpProviderImpl : LocalIpProvider {
    override fun getLocalSubnet(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.flatMap { it.inetAddresses.toList() }
                ?.firstOrNull { addr ->
                    !addr.isLoopbackAddress &&
                        addr.hostAddress?.contains('.') == true &&
                        addr.isSiteLocalAddress
                }
                ?.hostAddress
                ?.substringBeforeLast('.')
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
            // Network interface enumeration may fail on restricted environments
            null
        }
    }
}
