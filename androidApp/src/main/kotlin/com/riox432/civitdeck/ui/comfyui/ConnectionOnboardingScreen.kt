package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
        step.results.forEach { server ->
            MethodCard(
                title = server.displayName,
                description = "${server.ip}:${server.port}",
                onClick = { viewModel.onSelectDiscoveredServer(server) },
            )
        }
    }
    OutlinedButton(onClick = viewModel::onChooseMethod) {
        Text(stringResource(R.string.cd_navigate_back))
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
    Button(onClick = viewModel::onRetry, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.comfyui_onboarding_retry))
    }
    OutlinedButton(onClick = viewModel::onChooseMethod, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.cd_navigate_back))
    }
}

@Composable
private fun failureMessage(step: OnboardingStep.Failure): String = when (step.cause) {
    ConnectionFailureCause.Unreachable -> stringResource(R.string.comfyui_onboarding_fail_unreachable)
    ConnectionFailureCause.Timeout -> stringResource(R.string.comfyui_onboarding_fail_timeout)
    ConnectionFailureCause.Tls -> stringResource(R.string.comfyui_onboarding_fail_tls)
    ConnectionFailureCause.Http -> stringResource(
        R.string.comfyui_onboarding_fail_http,
        step.httpStatus ?: 0,
    )
    ConnectionFailureCause.Unknown -> stringResource(R.string.comfyui_onboarding_fail_unknown)
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
