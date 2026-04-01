package com.riox432.civitdeck.ui.dataset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatasetDetailTopBar(
    datasetName: String,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onBack: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onReviewDuplicates: () -> Unit,
    onResolutionFilter: () -> Unit,
    onExport: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = if (isSelectionMode) {
                    stringResource(
                        R.string.dataset_selected_count,
                        selectedCount
                    )
                } else {
                    datasetName
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = if (isSelectionMode) onClearSelection else onBack,
            ) {
                Icon(
                    imageVector = if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isSelectionMode) {
                        stringResource(R.string.cd_cancel_selection)
                    } else {
                        stringResource(R.string.cd_navigate_back)
                    },
                )
            }
        },
        actions = {
            if (isSelectionMode) {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.cd_select_all))
                }
            } else {
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.cd_export_dataset))
                }
                IconButton(onClick = onReviewDuplicates) {
                    Icon(Icons.Default.FindReplace, contentDescription = stringResource(R.string.cd_review_duplicates))
                }
                IconButton(onClick = onResolutionFilter) {
                    Icon(
                        Icons.Default.PhotoSizeSelectLarge,
                        contentDescription = stringResource(R.string.cd_resolution_filter)
                    )
                }
            }
        },
    )
}

@Composable
internal fun DatasetSelectionBottomBar(
    selectedCount: Int,
    onRemove: () -> Unit,
    onEditTags: () -> Unit,
) {
    BottomAppBar {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        ) {
            Button(onClick = onEditTags) {
                Icon(
                    imageVector = Icons.Default.Style,
                    contentDescription = stringResource(R.string.cd_edit_tags),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.dataset_edit_tags),
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
            Button(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_remove),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(
                        R.string.dataset_remove_images,
                        selectedCount,
                        if (selectedCount == 1) {
                            stringResource(
                                R.string.dataset_image_singular
                            )
                        } else {
                            stringResource(R.string.dataset_image_plural)
                        },
                    ),
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
    }
}
