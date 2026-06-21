package com.riox432.civitdeck.ui.update

import android.content.Intent

/**
 * Persistent UI state for the Android in-app self-update flow.
 *
 * Installation crosses Activity/process boundaries (unknown-sources settings, the system install
 * confirmation, and the [android.content.pm.PackageInstaller] result broadcast), so it is modelled
 * as a state machine rather than a single suspend call. One-shot, Activity-bound actions are
 * delivered separately via [UpdateInstallEffect].
 */
sealed interface UpdateInstallState {
    data object Idle : UpdateInstallState
    data class Downloading(val bytesRead: Long, val totalBytes: Long) : UpdateInstallState
    data object Verifying : UpdateInstallState
    data object NeedsUnknownAppsPermission : UpdateInstallState
    data object Installing : UpdateInstallState
    data object Success : UpdateInstallState
    data class Failed(val reason: InstallFailure) : UpdateInstallState
    data object Cancelled : UpdateInstallState
}

/** Reasons an install attempt can fail, mapped to a user-facing message at the UI layer. */
enum class InstallFailure {
    DOWNLOAD_FAILED,
    INTEGRITY_CHECK_FAILED,
    INSTALL_REJECTED,
    UNKNOWN,
}

/**
 * One-shot, Activity-bound side effects. The host Composable owns the launchers/Activity context and
 * executes these; the controller never holds an Activity reference.
 */
sealed interface UpdateInstallEffect {
    /** Route the user to the per-source "install unknown apps" settings screen. */
    data object OpenUnknownAppsSettings : UpdateInstallEffect

    /** Launch the system install confirmation dialog returned by PackageInstaller. */
    data class LaunchInstallConfirmation(val intent: Intent) : UpdateInstallEffect
}
