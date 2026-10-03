package com.riox432.civitdeck.ui.comfyui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ConnectionFailureCause
import com.riox432.civitdeck.feature.comfyui.presentation.ConnectionOnboardingViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.OnboardingStep
import com.riox432.civitdeck.feature.comfyui.presentation.OnboardingUiState
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionOnboardingScreen(
    viewModel: ConnectionOnboardingViewModel,
    onBack: () -> Unit,
    onConnected: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.comfyui_onboarding_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(Spacing.lg)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OnboardingContent(state, viewModel, onConnected)
        }
    }
}

@Composable
private fun OnboardingContent(
    state: OnboardingUiState,
    viewModel: ConnectionOnboardingViewModel,
    onConnected: () -> Unit,
) {
    when (val step = state.step) {
        is OnboardingStep.ChooseMethod -> MethodPicker(state, viewModel)
        is OnboardingStep.Scanning -> ScanningStep(step, viewModel)
        is OnboardingStep.Testing -> TestingStep(step)
        is OnboardingStep.Success -> SuccessStep(step, onConnected)
        is OnboardingStep.Failure -> FailureStep(step, viewModel)
    }
}

@Composable
private fun MethodPicker(state: OnboardingUiState, viewModel: ConnectionOnboardingViewModel) {
    Text(
        stringResource(R.string.comfyui_onboarding_choose_method),
        style = MaterialTheme.typography.bodyMedium,
    )
    if (state.lanScanSupported) {
        MethodCard(
            title = stringResource(R.string.comfyui_onboarding_method_detect),
            description = stringResource(R.string.comfyui_onboarding_method_detect_desc),
            onClick = viewModel::onStartScan,
        )
    }
    ManualEntryForm(viewModel)
}

@Composable
private fun MethodCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ScanningStep(step: OnboardingStep.Scanning, viewModel: ConnectionOnboardingViewModel) {
    if (step.isComplete) {
        ScanFinishedContent(step, viewModel)
    } else {
        ScanInProgressContent(step, viewModel)
    }
}

@Composable
private fun ScanInProgressContent(step: OnboardingStep.Scanning, viewModel: ConnectionOnboardingViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.comfyui_onboarding_scanning))
    }
    if (step.results.isEmpty()) {
        Text(
            stringResource(R.string.comfyui_onboarding_no_servers),
            style = MaterialTheme.typography.bodySmall,
        )
    } else {
        DiscoveredServerCards(step, viewModel)
    }
    OutlinedButton(onClick = viewModel::onChooseMethod) {
        Text(stringResource(R.string.cd_navigate_back))
    }
}

@Composable
private fun ScanFinishedContent(step: OnboardingStep.Scanning, viewModel: ConnectionOnboardingViewModel) {
    if (step.results.isEmpty()) {
        Text(
            stringResource(R.string.comfyui_onboarding_scan_none),
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        DiscoveredServerCards(step, viewModel)
    }
    // Equal weights let a label wrap instead of pushing the other button off-screen at large font scales.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedButton(onClick = viewModel::onChooseMethod, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.cd_navigate_back))
        }
        Button(onClick = viewModel::onStartScan, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.comfyui_onboarding_scan_again))
        }
    }
}

@Composable
private fun DiscoveredServerCards(step: OnboardingStep.Scanning, viewModel: ConnectionOnboardingViewModel) {
    step.results.forEach { server ->
        MethodCard(
            title = server.displayName,
            description = "${server.ip}:${server.port}",
            onClick = { viewModel.onSelectDiscoveredServer(server) },
        )
    }
}

@Composable
private fun TestingStep(step: OnboardingStep.Testing) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(R.string.comfyui_onboarding_testing, step.connection.hostname),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SuccessStep(step: OnboardingStep.Success, onConnected: () -> Unit) {
    Text(
        stringResource(R.string.comfyui_onboarding_connected),
        style = MaterialTheme.typography.titleLarge,
    )
    Text(
        stringResource(R.string.comfyui_onboarding_connected_desc, step.connection.name),
        style = MaterialTheme.typography.bodyMedium,
    )
    step.stats?.let { stats ->
        Text("${stats.gpuName} • ${stats.vramTotalMB} MB VRAM", style = MaterialTheme.typography.bodySmall)
    }
    Button(onClick = onConnected, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.comfyui_onboarding_done))
    }
}

@Composable
private fun FailureStep(step: OnboardingStep.Failure, viewModel: ConnectionOnboardingViewModel) {
    Text(failureMessage(step), style = MaterialTheme.typography.bodyMedium)
    val fingerprint = step.presentedSha256?.takeIf { step.cause in CERTIFICATE_CAUSES }
    if (fingerprint != null) {
        CertificateFingerprint(fingerprint)
        Button(onClick = viewModel::onTrustCertificate, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.comfyui_onboarding_trust_certificate))
        }
        OutlinedButton(onClick = viewModel::onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.comfyui_onboarding_retry))
        }
    } else {
        Button(onClick = viewModel::onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.comfyui_onboarding_retry))
        }
    }
    OutlinedButton(onClick = viewModel::onChooseMethod, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.cd_navigate_back))
    }
}

@Composable
private fun CertificateFingerprint(sha256Hex: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                stringResource(R.string.comfyui_onboarding_cert_fingerprint_label),
                style = MaterialTheme.typography.titleSmall,
            )
            // Wraps instead of truncating so every byte stays comparable with the server's output.
            Text(
                formatSha256Fingerprint(sha256Hex),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
            Text(
                stringResource(R.string.comfyui_onboarding_cert_fingerprint_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * Formats a lowercase hex SHA-256 digest the way `openssl x509 -noout -fingerprint -sha256`
 * prints it (colon-separated uppercase byte pairs), so the user can compare the two directly.
 */
internal fun formatSha256Fingerprint(sha256Hex: String): String =
    sha256Hex.uppercase().chunked(2).joinToString(":")

private val CERTIFICATE_CAUSES = setOf(
    ConnectionFailureCause.CertificateUnconfirmed,
    ConnectionFailureCause.CertificateChanged,
)

/** Causes whose message takes the HTTP status code as its `%1$d` argument. */
private val HTTP_STATUS_CAUSES = setOf(
    ConnectionFailureCause.Http,
    ConnectionFailureCause.AuthRequired,
)

@Composable
private fun failureMessage(step: OnboardingStep.Failure): String {
    val resId = failureMessageRes(step.cause)
    return if (step.cause in HTTP_STATUS_CAUSES) {
        stringResource(resId, step.httpStatus ?: 0)
    } else {
        stringResource(resId)
    }
}

@StringRes
internal fun failureMessageRes(cause: ConnectionFailureCause): Int = when (cause) {
    // LocalNetworkDenied is only reported on iOS.
    ConnectionFailureCause.Unreachable,
    ConnectionFailureCause.LocalNetworkDenied,
    -> R.string.comfyui_onboarding_fail_unreachable
    ConnectionFailureCause.Refused -> R.string.comfyui_onboarding_fail_refused
    ConnectionFailureCause.LoopbackHost -> R.string.comfyui_onboarding_fail_loopback
    ConnectionFailureCause.Timeout -> R.string.comfyui_onboarding_fail_timeout
    ConnectionFailureCause.Tls -> R.string.comfyui_onboarding_fail_tls
    ConnectionFailureCause.CertificateUnconfirmed -> R.string.comfyui_onboarding_fail_cert_unconfirmed
    ConnectionFailureCause.CertificateChanged -> R.string.comfyui_onboarding_fail_cert_changed
    ConnectionFailureCause.AuthRequired -> R.string.comfyui_onboarding_fail_auth
    ConnectionFailureCause.Http -> R.string.comfyui_onboarding_fail_http
    ConnectionFailureCause.NotComfyUI -> R.string.comfyui_onboarding_fail_not_comfyui
    ConnectionFailureCause.Unknown -> R.string.comfyui_onboarding_fail_unknown
}

@Composable
private fun ManualEntryForm(viewModel: ConnectionOnboardingViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var host by rememberSaveable { mutableStateOf("") }
    var portText by rememberSaveable {
        mutableStateOf(ComfyUIConnection.DEFAULT_COMFYUI_PORT.toString())
    }
    var useHttps by rememberSaveable { mutableStateOf(false) }
    var acceptSelfSigned by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                stringResource(R.string.comfyui_onboarding_method_manual),
                style = MaterialTheme.typography.titleMedium,
            )
            ManualEntryFields(
                name = name,
                onNameChange = { name = it },
                host = host,
                onHostChange = { host = it },
                portText = portText,
                onPortChange = { portText = it.filter(Char::isDigit) },
            )
            SwitchRow(stringResource(R.string.comfyui_use_https), useHttps) { useHttps = it }
            SwitchRow(
                stringResource(R.string.comfyui_accept_self_signed),
                acceptSelfSigned,
            ) { acceptSelfSigned = it }
            Button(
                onClick = {
                    viewModel.onManualSubmit(
                        name = name,
                        hostname = host.trim(),
                        port = portText.toIntOrNull() ?: ComfyUIConnection.DEFAULT_COMFYUI_PORT,
                        useHttps = useHttps,
                        acceptSelfSigned = acceptSelfSigned,
                    )
                },
                enabled = host.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.comfyui_onboarding_connect))
            }
        }
    }
}

@Composable
private fun ManualEntryFields(
    name: String,
    onNameChange: (String) -> Unit,
    host: String,
    onHostChange: (String) -> Unit,
    portText: String,
    onPortChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.comfyui_onboarding_name_label)) },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = host,
        onValueChange = onHostChange,
        label = { Text(stringResource(R.string.comfyui_hostname_label)) },
        placeholder = { Text(stringResource(R.string.comfyui_hostname_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = portText,
        onValueChange = onPortChange,
        label = { Text(stringResource(R.string.comfyui_onboarding_port_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
