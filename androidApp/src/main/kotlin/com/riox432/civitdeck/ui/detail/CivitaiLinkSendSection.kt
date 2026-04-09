package com.riox432.civitdeck.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.data.api.CivitAiUrls
import com.riox432.civitdeck.domain.model.CivitaiLinkResource
import com.riox432.civitdeck.domain.model.CivitaiLinkStatus
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.feature.comfyui.presentation.CivitaiLinkSendViewModel
import com.riox432.civitdeck.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CivitaiLinkSendSheet(
    model: Model?,
    selectedVersionIndex: Int,
    onDismiss: () -> Unit,
) {
    val sendViewModel: CivitaiLinkSendViewModel = koinViewModel()
    val status by sendViewModel.status.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        CivitaiLinkSendSheetContent(
            model = model,
            selectedVersionIndex = selectedVersionIndex,
            status = status,
            onSend = { resource ->
                sendViewModel.sendToPC(resource)
                onDismiss()
            },
        )
    }
}

@Composable
private fun CivitaiLinkSendSheetContent(
    model: Model?,
    selectedVersionIndex: Int,
    status: CivitaiLinkStatus,
    onSend: (CivitaiLinkResource) -> Unit,
) {
    if (status != CivitaiLinkStatus.Connected) {
        CivitaiLinkNotConnectedMessage()
        return
    }
    val safeModel = model ?: return
    val version = safeModel.modelVersions.getOrNull(selectedVersionIndex) ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text("Send to PC", style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = {
                onSend(
                    CivitaiLinkResource(
                        versionId = version.id,
                        modelId = safeModel.id,
                        versionName = version.name,
                        downloadUrl = CivitAiUrls.downloadUrl(version.id),
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Send ${version.name} to PC")
        }
    }
}

@Composable
private fun CivitaiLinkNotConnectedMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Civitai Link not configured", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Set up Civitai Link in Settings \u2192 Advanced to send models to your PC",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
