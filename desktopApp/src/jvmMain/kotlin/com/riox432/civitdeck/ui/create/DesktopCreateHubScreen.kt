package com.riox432.civitdeck.ui.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.ui.theme.Spacing

private data class CreateHubItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

private val CREATE_ITEMS = listOf(
    CreateHubItem("ComfyUI", "Node-based generation workflow", Icons.Default.Brush),
    CreateHubItem("SD WebUI", "Stable Diffusion WebUI generation", Icons.Default.Cloud),
    CreateHubItem("External Server", "Custom server connection", Icons.Default.Dns),
)

@Composable
fun DesktopCreateHubScreen(
    onComfyUIClick: () -> Unit,
    onSDWebUIClick: () -> Unit,
    onExternalServerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val callbacks = listOf(onComfyUIClick, onSDWebUIClick, onExternalServerClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = "Create",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )
        Text(
            text = "Generation tools and workflows",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.md),
        )
        CREATE_ITEMS.forEachIndexed { index, item ->
            CreateHubCard(
                item = item,
                onClick = callbacks[index],
            )
        }
    }
}

@Composable
private fun CreateHubCard(
    item: CreateHubItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
