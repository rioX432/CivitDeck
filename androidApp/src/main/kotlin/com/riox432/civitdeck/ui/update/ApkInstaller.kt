package com.riox432.civitdeck.ui.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Low-level Android APK download + integrity verification + install via [PackageInstaller].
 *
 * Lives in the app layer (not in common/domain) because the install flow is inherently
 * Activity/Context-bound and asynchronous — see [UpdateInstallState]. Uses only the application
 * context; the Activity-bound steps are surfaced as effects and handled by the host Composable.
 */
class ApkInstaller(private val context: Context) {

    fun canRequestInstalls(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /**
     * Downloads the APK to app-private cache, reporting progress via [onProgress]. Performs blocking
     * IO — callers must invoke it off the main thread.
     */
    fun download(url: String, onProgress: (bytesRead: Long, total: Long) -> Unit): File {
        val dir = File(context.cacheDir, UPDATE_DIR).apply { mkdirs() }
        val target = File(dir, APK_NAME)
        if (target.exists()) target.delete()

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
        }
        try {
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                target.outputStream().use { out -> copyWithProgress(input, out, total, onProgress) }
            }
        } finally {
            connection.disconnect()
        }
        return target
    }

    private fun copyWithProgress(
        input: java.io.InputStream,
        out: java.io.OutputStream,
        total: Long,
        onProgress: (bytesRead: Long, total: Long) -> Unit,
    ) {
        val buffer = ByteArray(DOWNLOAD_CHUNK)
        var read = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            out.write(buffer, 0, count)
            read += count
            onProgress(read, total)
        }
    }

    /**
     * Verifies the downloaded APK is a genuine, matching build of this app: package name must match
     * and the signing certificate set must be identical to the running app. This is the primary
     * integrity gate — it defends against a swapped APK even if release metadata were tampered with.
     */
    @Suppress("DEPRECATION")
    fun verify(apk: File): Boolean {
        val pm = context.packageManager
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val downloaded = pm.getPackageArchiveInfo(apk.absolutePath, flags) ?: return false
        if (downloaded.packageName != context.packageName) return false

        val installed = pm.getPackageInfo(context.packageName, flags)
        // Reject downgrades / equal builds — only a strictly newer APK is installable here.
        if (downloaded.longVersionCode <= installed.longVersionCode) return false
        return signersMatch(downloaded.signingInfo, installed.signingInfo)
    }

    /**
     * Accepts the downloaded APK when its current signer set equals the installed one, or — for
     * single-signer apps — when the downloaded signing lineage includes the currently installed
     * signer (supports APK Signature Scheme key rotation).
     */
    private fun signersMatch(downloaded: SigningInfo?, installed: SigningInfo?): Boolean {
        if (downloaded == null || installed == null) return false
        val downloadedCurrent = downloaded.apkContentsSigners.toCertSet()
        val installedCurrent = installed.apkContentsSigners.toCertSet()
        if (downloadedCurrent.isEmpty() || installedCurrent.isEmpty()) return false
        if (downloadedCurrent == installedCurrent) return true

        if (downloaded.hasMultipleSigners() || installed.hasMultipleSigners()) return false
        val lineage = downloaded.signingCertificateHistory?.toCertSet().orEmpty()
        return lineage.isNotEmpty() && installedCurrent.all { it in lineage }
    }

    /**
     * Streams the APK into a [PackageInstaller] session and commits it. The result (and any required
     * user confirmation) is delivered to the broadcast receiver listening for [statusAction].
     */
    fun commit(apk: File, statusAction: String) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL,
        )
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite(APK_NAME, 0, apk.length()).use { out ->
                apk.inputStream().use { input -> input.copyTo(out) }
                session.fsync(out)
            }
            session.commit(buildStatusSender(sessionId, statusAction).intentSender)
        }
    }

    fun cleanup() {
        File(context.cacheDir, UPDATE_DIR).deleteRecursively()
    }

    private fun buildStatusSender(sessionId: Int, statusAction: String): PendingIntent {
        val intent = Intent(statusAction).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context,
            sessionId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private fun Array<Signature>.toCertSet(): Set<String> {
        val digest = MessageDigest.getInstance("SHA-256")
        return map { digest.digest(it.toByteArray()).joinToString("") { b -> "%02x".format(b) } }
            .toSet()
    }

    companion object {
        private const val UPDATE_DIR = "app_update"
        private const val APK_NAME = "update.apk"
        private const val DOWNLOAD_CHUNK = 64 * 1024
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 30_000
    }
}
