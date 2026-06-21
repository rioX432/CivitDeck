package com.riox432.civitdeck.ui.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/**
 * Drives the Android in-app self-update state machine: download -> verify -> (permission) -> install.
 *
 * Holds only the application context; all Activity-bound steps are surfaced through [effects] and
 * executed by the host Composable. The PackageInstaller commit result is delivered asynchronously
 * through a runtime [BroadcastReceiver] and folded back into [state].
 */
class UpdateInstallController(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val installer = ApkInstaller(appContext)
    private val statusAction = "${appContext.packageName}.APK_INSTALL_STATUS"

    private val _state = MutableStateFlow<UpdateInstallState>(UpdateInstallState.Idle)
    val state: StateFlow<UpdateInstallState> = _state.asStateFlow()

    private val _effects = Channel<UpdateInstallEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var pendingApk: File? = null
    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != statusAction) return
            handleStatus(intent)
        }
    }

    /** Starts a fresh install attempt. No-ops while one is already in progress. */
    fun start(downloadUrl: String) {
        if (_state.value.isInProgress()) return
        scope.launch {
            try {
                val apk = downloadAndVerify(downloadUrl) ?: return@launch
                pendingApk = apk
                proceedToInstall()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(InstallFailure.DOWNLOAD_FAILED, e)
            }
        }
    }

    /** Called by the host after the user returns from the unknown-app-sources settings screen. */
    fun onUnknownSourcesResult() {
        val apk = pendingApk ?: return
        if (installer.canRequestInstalls()) {
            scope.launch { commit(apk) }
        } else {
            _state.value = UpdateInstallState.Cancelled
            reset()
        }
    }

    fun dismiss() {
        _state.value = UpdateInstallState.Idle
        reset()
    }

    fun dispose() = reset()

    private suspend fun downloadAndVerify(url: String): File? {
        _state.value = UpdateInstallState.Downloading(0, 0)
        val apk = runInterruptible(Dispatchers.IO) {
            installer.download(url) { read, total ->
                _state.value = UpdateInstallState.Downloading(read, total)
            }
        }
        _state.value = UpdateInstallState.Verifying
        val valid = runInterruptible(Dispatchers.IO) { installer.verify(apk) }
        if (!valid) {
            fail(InstallFailure.INTEGRITY_CHECK_FAILED)
            return null
        }
        return apk
    }

    private suspend fun proceedToInstall() {
        val apk = pendingApk ?: return
        if (installer.canRequestInstalls()) {
            commit(apk)
        } else {
            _state.value = UpdateInstallState.NeedsUnknownAppsPermission
            _effects.send(UpdateInstallEffect.OpenUnknownAppsSettings)
        }
    }

    private suspend fun commit(apk: File) {
        registerReceiver()
        _state.value = UpdateInstallState.Installing
        withContext(Dispatchers.IO) { installer.commit(apk, statusAction) }
    }

    private fun handleStatus(intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                val delivered = confirm != null &&
                    _effects.trySend(UpdateInstallEffect.LaunchInstallConfirmation(confirm)).isSuccess
                if (!delivered) fail(InstallFailure.UNKNOWN)
            }
            PackageInstaller.STATUS_SUCCESS -> {
                _state.value = UpdateInstallState.Success
                reset()
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                _state.value = UpdateInstallState.Cancelled
                reset()
            }
            else -> fail(InstallFailure.INSTALL_REJECTED)
        }
    }

    private fun fail(reason: InstallFailure, error: Exception? = null) {
        if (error != null) Logger.w(TAG, "Install failed: ${error.message}")
        _state.value = UpdateInstallState.Failed(reason)
        reset()
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(statusAction),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun reset() {
        if (receiverRegistered) {
            runCatching { appContext.unregisterReceiver(receiver) }
            receiverRegistered = false
        }
        pendingApk = null
        installer.cleanup()
    }

    private fun UpdateInstallState.isInProgress(): Boolean = when (this) {
        is UpdateInstallState.Downloading,
        UpdateInstallState.Verifying,
        UpdateInstallState.NeedsUnknownAppsPermission,
        UpdateInstallState.Installing,
        -> true
        else -> false
    }

    private companion object {
        const val TAG = "UpdateInstallController"
    }
}
