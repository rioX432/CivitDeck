package com.riox432.civitdeck.ui.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHubScreen(
    onNavigateToComfyUI: () -> Unit,
    onNavigateToSDWebUI: () -> Unit,
    onNavigateToExternalServer: () -> Unit,
    onNavigateToModelFiles: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Create") })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                CreateToolItem(
                    icon = Icons.Default.Palette,
                    title = "ComfyUI",
                    subtitle = "Node-based image generation workflow",
                    onClick = onNavigateToComfyUI,
                )
            }
            item {
                CreateToolItem(
                    icon = Icons.Default.Memory,
                    title = "SD WebUI",
                    subtitle = "Stable Diffusion web interface",
                    onClick = onNavigateToSDWebUI,
                )
            }
            item {
                CreateToolItem(
                    icon = Icons.Default.Dns,
                    title = "External Server",
                    subtitle = "Connect to custom generation servers",
                    onClick = onNavigateToExternalServer,
                )
            }
            item {
                CreateToolItem(
                    icon = Icons.Default.Folder,
                    title = "Model File Browser",
                    subtitle = "Browse and manage local model files",
                    onClick = onNavigateToModelFiles,
                )
            }
        }
    }
}

@Composable
private fun CreateToolItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
