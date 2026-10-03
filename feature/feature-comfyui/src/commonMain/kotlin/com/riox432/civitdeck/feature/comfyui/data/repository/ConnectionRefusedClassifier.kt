package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.domain.model.ConnectionFailureCause

private const val CONNECTION_REFUSED = "Connection refused"
private const val NS_URL_ERROR_DOMAIN = "NSURLErrorDomain"

// NSURLErrorCannotConnectToHost, as it appears in Foundation's NSError description.
private const val NS_URL_ERROR_CANNOT_CONNECT_TO_HOST = "Code=-1004"

// NSURLErrorNotConnectedToInternet, as it appears in Foundation's NSError description.
private const val NS_URL_ERROR_NOT_CONNECTED_TO_INTERNET = "Code=-1009"

// Guards against a cyclic cause chain, which Throwable does not prevent beyond a direct self-cause.
private const val MAX_CAUSE_DEPTH = 16

private const val MDNS_SUFFIX = ".local"
private val IPV4_ADDRESS = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")
private const val MAX_OCTET = 255

/**
 * Classifies a transport failure that produced no HTTP response. Each engine reports a refused TCP
 * connection (typically ComfyUI started without `--listen`) differently: JVM and OkHttp messages
 * say `Connection refused` (OkHttp nests `ECONNREFUSED (Connection refused)` in a cause), and Ktor
 * Darwin embeds the NSError description. The messages are matched in common code, like the iOS TLS
 * classifier, so the logic stays unit-testable without the Darwin engine. Anything else, including
 * DNS and no-route errors, stays [ConnectionFailureCause.Unreachable].
 *
 * iOS reports a denied Local Network permission only as `NSURLErrorNotConnectedToInternet` (-1009),
 * and offers no API to read the permission state, so -1009 against a LAN [hostname] is reported as
 * [ConnectionFailureCause.LocalNetworkDenied]. The permission does not cover VPN peers such as
 * Tailscale (Apple TN3179), so -1009 against any other host stays Unreachable.
 */
internal fun transportFailureCause(throwable: Throwable, hostname: String): ConnectionFailureCause = when {
    isConnectionRefused(throwable) -> ConnectionFailureCause.Refused
    isNotConnectedToInternet(throwable) && isLanHost(hostname) -> ConnectionFailureCause.LocalNetworkDenied
    else -> ConnectionFailureCause.Unreachable
}

private fun isConnectionRefused(throwable: Throwable): Boolean = anyMessageInCauseChain(throwable) { message ->
    message.contains(CONNECTION_REFUSED, ignoreCase = true) ||
        (NS_URL_ERROR_DOMAIN in message && NS_URL_ERROR_CANNOT_CONNECT_TO_HOST in message)
}

private fun isNotConnectedToInternet(throwable: Throwable): Boolean = anyMessageInCauseChain(throwable) { message ->
    NS_URL_ERROR_DOMAIN in message && NS_URL_ERROR_NOT_CONNECTED_TO_INTERNET in message
}

private fun anyMessageInCauseChain(throwable: Throwable, predicate: (String) -> Boolean): Boolean =
    generateSequence(throwable) { it.cause }.take(MAX_CAUSE_DEPTH).any { predicate(it.message.orEmpty()) }

/** RFC 1918 private ranges, IPv4 link-local, or an mDNS `.local` name. */
private fun isLanHost(hostname: String): Boolean {
    val host = hostname.trim().removeSuffix(".")
    if (host.endsWith(MDNS_SUFFIX, ignoreCase = true) && host.length > MDNS_SUFFIX.length) return true
    val octets = IPV4_ADDRESS.matchEntire(host)?.groupValues?.drop(1)?.map { it.toInt() } ?: return false
    if (octets.any { it > MAX_OCTET }) return false
    val (first, second) = octets
    return first == 10 ||
        (first == 172 && second in 16..31) ||
        (first == 192 && second == 168) ||
        (first == 169 && second == 254)
}
