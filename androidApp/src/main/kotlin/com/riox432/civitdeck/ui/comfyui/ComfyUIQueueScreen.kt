package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.QueueJob
import com.riox432.civitdeck.domain.model.QueueJobStatus
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIQueueViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.QueueUiState
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfyUIQueueScreen(
    viewModel: ComfyUIQueueViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Queue") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        QueueBody(
            state = state,
            onCancel = viewModel::onCancelJob,
            onRetry = viewModel::dismissError,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun QueueBody(
    state: QueueUiState,
    onCancel: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && state.jobs.isEmpty() -> LoadingStateOverlay()
            state.error != null && state.jobs.isEmpty() -> {
                ErrorStateView(
                    message = state.error ?: "Error",
                    onRetry = onRetry,
                )
            }
            state.jobs.isEmpty() -> {
                EmptyStateMessage(
                    icon = Icons.Filled.Cancel,
                    title = "Queue is empty",
                    subtitle = "No jobs are running or pending.",
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(state.jobs, key = { it.promptId }) { job ->
                        QueueJobItem(
                            job = job,
                            isCancelling = job.promptId in state.cancellingIds,
                            onCancel = { onCancel(job.promptId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueJobItem(
    job: QueueJob,
    isCancelling: Boolean,
    onCancel: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.promptId.take(JOB_ID_DISPLAY_LENGTH) + "...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = statusLabel(job.status),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(job.status),
                )
            }
            if (isCancelling) {
                CircularProgressIndicator(modifier = Modifier.padding(Spacing.sm))
            } else if (job.status != QueueJobStatus.Completed) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Cancel job",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: QueueJobStatus): String = when (status) {
    QueueJobStatus.Queued -> "Queued"
    QueueJobStatus.Running -> "Running"
    QueueJobStatus.Completed -> "Completed"
    QueueJobStatus.Error -> "Error"
}

@Composable
private fun statusColor(status: QueueJobStatus) = when (status) {
    QueueJobStatus.Running -> MaterialTheme.colorScheme.primary
    QueueJobStatus.Error -> MaterialTheme.colorScheme.error
    QueueJobStatus.Completed -> MaterialTheme.colorScheme.tertiary
    QueueJobStatus.Queued -> MaterialTheme.colorScheme.onSurfaceVariant
}

private const val JOB_ID_DISPLAY_LENGTH = 8
