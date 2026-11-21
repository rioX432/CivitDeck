package com.riox432.civitdeck.domain.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.riox432.civitdeck.domain.model.ComfyUIJobHandle
import com.riox432.civitdeck.domain.model.ComfyUIJobTarget
import com.riox432.civitdeck.util.Logger

class BackgroundMonitorStarterImpl(
    private val context: Context,
) : BackgroundMonitorStarter {
    override fun startMonitoring(promptId: String, baseUrl: String, wsScheme: String) {
        startMonitorService(
            mapOf(EXTRA_PROMPT_ID to promptId, EXTRA_BASE_URL to baseUrl, EXTRA_WS_SCHEME to wsScheme),
        )
    }

    override fun startMonitoring(job: ComfyUIJobHandle) {
        startMonitorService(MonitorJobExtras.encode(job))
    }

    private fun startMonitorService(extras: Map<String, String>) {
        val intent = Intent().apply {
            setClassName(context, SERVICE_CLASS_NAME)
            action = ACTION_START
            extras.forEach { (key, value) -> putExtra(key, value) }
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Failed to start monitor service: ${e.message}")
        }
    }

    override fun stopMonitoring() {
        val intent = Intent().apply {
            setClassName(context, SERVICE_CLASS_NAME)
            action = ACTION_STOP
        }
        try {
            context.startService(intent)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Failed to stop monitor service: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "BackgroundMonitorStarter"
        private const val SERVICE_CLASS_NAME =
            "com.riox432.civitdeck.service.GenerationMonitorService"
        const val ACTION_START = "com.riox432.civitdeck.action.START_MONITOR"
        const val ACTION_STOP = "com.riox432.civitdeck.action.STOP_MONITOR"
        const val EXTRA_PROMPT_ID = "extra_prompt_id"
        const val EXTRA_BASE_URL = "extra_base_url"
        const val EXTRA_WS_SCHEME = "extra_ws_scheme"
    }
}

/**
 * Carries a bound job to the monitor service as string extras, so the service rebuilds the exact
 * target the job was submitted to (connection ID, address and TLS pin) instead of deriving trust
 * from a URL or reading the active connection. Plain maps keep the codec testable on the host;
 * the Intent is adapted at the starter and the service.
 */
object MonitorJobExtras {
    const val EXTRA_CONNECTION_ID = "extra_target_connection_id"
    const val EXTRA_HOSTNAME = "extra_target_hostname"
    const val EXTRA_PORT = "extra_target_port"
    const val EXTRA_USE_HTTPS = "extra_target_use_https"
    const val EXTRA_ACCEPT_SELF_SIGNED = "extra_target_accept_self_signed"
    const val EXTRA_TLS_CERT_SHA256 = "extra_target_tls_cert_sha256"

    private val TARGET_KEYS = listOf(
        EXTRA_CONNECTION_ID,
        EXTRA_HOSTNAME,
        EXTRA_PORT,
        EXTRA_USE_HTTPS,
        EXTRA_ACCEPT_SELF_SIGNED,
        EXTRA_TLS_CERT_SHA256,
    )
    private const val MAX_PORT = 65_535

    sealed interface Decoded {
        /** No target extras: a start from the prompt-ID/base-URL entry point. */
        data object Legacy : Decoded
        data class Bound(val job: ComfyUIJobHandle) : Decoded
        data class Malformed(val reason: String) : Decoded
    }

    fun encode(job: ComfyUIJobHandle): Map<String, String> = buildMap {
        val target = job.target
        put(BackgroundMonitorStarterImpl.EXTRA_PROMPT_ID, job.promptId)
        put(EXTRA_CONNECTION_ID, target.connectionId.toString())
        put(EXTRA_HOSTNAME, target.hostname)
        put(EXTRA_PORT, target.port.toString())
        put(EXTRA_USE_HTTPS, target.useHttps.toString())
        put(EXTRA_ACCEPT_SELF_SIGNED, target.acceptSelfSigned.toString())
        target.tlsCertSha256?.let { put(EXTRA_TLS_CERT_SHA256, it) }
    }

    /** Reads every key [decode] understands through [get], e.g. `intent::getStringExtra`. */
    fun read(get: (String) -> String?): Map<String, String> =
        (TARGET_KEYS + BackgroundMonitorStarterImpl.EXTRA_PROMPT_ID)
            .mapNotNull { key -> get(key)?.let { key to it } }
            .toMap()

    /**
     * Once any target key is present the payload must decode completely: a partial target is
     * reported as [Decoded.Malformed] rather than monitored without its address or pin.
     */
    fun decode(extras: Map<String, String>): Decoded {
        if (TARGET_KEYS.none(extras::containsKey)) return Decoded.Legacy
        return try {
            Decoded.Bound(parse(extras))
        } catch (e: IllegalArgumentException) {
            Decoded.Malformed(e.message ?: "invalid monitor target")
        }
    }

    private fun parse(extras: Map<String, String>): ComfyUIJobHandle {
        fun <T : Any> field(key: String, name: String, parse: (String) -> T?): T =
            requireNotNull(extras[key]?.let(parse)) { "missing or invalid $name" }

        val pin = extras[EXTRA_TLS_CERT_SHA256]
        require(pin == null || pin.isNotBlank()) { "blank TLS pin" }
        val target = ComfyUIJobTarget(
            connectionId = field(EXTRA_CONNECTION_ID, "connection ID", String::toLongOrNull),
            hostname = field(EXTRA_HOSTNAME, "hostname") { it.takeIf(String::isNotBlank) },
            port = field(EXTRA_PORT, "port") { it.toIntOrNull()?.takeIf { port -> port in 1..MAX_PORT } },
            useHttps = field(EXTRA_USE_HTTPS, "HTTPS flag", String::toBooleanStrictOrNull),
            acceptSelfSigned = field(EXTRA_ACCEPT_SELF_SIGNED, "self-signed flag", String::toBooleanStrictOrNull),
            tlsCertSha256 = pin,
        )
        val promptId = field(BackgroundMonitorStarterImpl.EXTRA_PROMPT_ID, "prompt ID") {
            it.takeIf(String::isNotBlank)
        }
        return ComfyUIJobHandle(target, promptId)
    }
}
