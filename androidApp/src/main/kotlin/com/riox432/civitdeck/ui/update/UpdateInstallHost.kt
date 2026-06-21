package com.riox432.civitdeck.ui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R

/** Remembers a [UpdateInstallController] scoped to the current composition. */
@Composable
fun rememberUpdateInstallController(): UpdateInstallController {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val controller = remember { UpdateInstallController(context, scope) }
    DisposableEffect(Unit) { onDispose { controller.dispose() } }
    return controller
}

/**
 * Owns the Activity-bound mechanics of the install flow: launches the unknown-app-sources settings
 * screen (re-checking permission on return) and the system install confirmation, then renders the
 * install progress/error dialog. The controller itself never touches an Activity.
 */
@Composable
fun UpdateInstallHost(controller: UpdateInstallController) {
    val context = LocalContext.current
    val state by controller.state.collectAsStateWithLifecycle()

    val settingsLauncher = rememberLauncherForActivityResult(StartActivityForResult()) {
        controller.onUnknownSourcesResult()
    }

    LaunchedEffect(controller) {
        controller.effects.collect { effect ->
            when (effect) {
                UpdateInstallEffect.OpenUnknownAppsSettings ->
                    settingsLauncher.launch(unknownSourcesIntent(context))
                is UpdateInstallEffect.LaunchInstallConfirmation ->
                    context.startActivity(effect.intent)
            }
        }
    }

    UpdateInstallDialog(state = state, onDismiss = controller::dismiss)
}

@Composable
private fun UpdateInstallDialog(state: UpdateInstallState, onDismiss: () -> Unit) {
    val message = state.messageRes() ?: return
    val dismissible = state.isDismissible()
    AlertDialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
        ),
        title = { Text(stringResource(R.string.update_install_title)) },
        text = {
            when (state) {
                is UpdateInstallState.Downloading -> DownloadProgress(state)
                else -> Text(stringResource(message))
            }
        },
        confirmButton = {
            if (dismissible) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_dismiss)) }
            }
        },
    )
}

@Composable
private fun DownloadProgress(state: UpdateInstallState.Downloading) {
    if (state.totalBytes > 0) {
        LinearProgressIndicator(progress = { state.bytesRead.toFloat() / state.totalBytes })
    } else {
        LinearProgressIndicator()
    }
}

private fun unknownSourcesIntent(context: Context): Intent =
    Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}"),
    )

private fun UpdateInstallState.messageRes(): Int? = when (this) {
    is UpdateInstallState.Downloading -> R.string.update_downloading
    UpdateInstallState.Verifying -> R.string.update_verifying
    UpdateInstallState.NeedsUnknownAppsPermission -> R.string.update_permission_needed
    UpdateInstallState.Installing -> R.string.update_installing
    is UpdateInstallState.Failed -> reason.messageRes()
    UpdateInstallState.Idle,
    UpdateInstallState.Success,
    UpdateInstallState.Cancelled,
    -> null
}

private fun InstallFailure.messageRes(): Int = when (this) {
    InstallFailure.DOWNLOAD_FAILED -> R.string.update_failed_download
    InstallFailure.INTEGRITY_CHECK_FAILED -> R.string.update_failed_integrity
    InstallFailure.INSTALL_REJECTED -> R.string.update_failed_rejected
    InstallFailure.UNKNOWN -> R.string.update_failed_unknown
}

private fun UpdateInstallState.isDismissible(): Boolean = this is UpdateInstallState.Failed
