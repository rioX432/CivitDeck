package com.riox432.civitdeck.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitReviewSheet(
    isSubmitting: Boolean,
    onSubmit: (rating: Int, recommended: Boolean, details: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rating by remember { mutableIntStateOf(0) }
    var recommended by remember { mutableStateOf(true) }
    var details by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        SubmitReviewContent(
            rating = rating,
            recommended = recommended,
            details = details,
            isSubmitting = isSubmitting,
            onRatingChange = { rating = it },
            onRecommendedChange = { recommended = it },
            onDetailsChange = { details = it },
            onSubmit = { onSubmit(rating, recommended, details.ifBlank { null }) },
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun SubmitReviewContent(
    rating: Int,
    recommended: Boolean,
    details: String,
    isSubmitting: Boolean,
    onRatingChange: (Int) -> Unit,
    onRecommendedChange: (Boolean) -> Unit,
    onDetailsChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg)
            .padding(bottom = Spacing.xl),
    ) {
        Text("Write a Review", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.lg))
        StarRatingSelector(rating = rating, onRatingChange = onRatingChange)
        Spacer(Modifier.height(Spacing.md))
        RecommendationSelector(recommended = recommended, onChanged = onRecommendedChange)
        Spacer(Modifier.height(Spacing.md))
        OutlinedTextField(
            value = details,
            onValueChange = onDetailsChange,
            label = { Text("Details (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = MIN_TEXT_LINES,
            maxLines = MAX_TEXT_LINES,
        )
        Spacer(Modifier.height(Spacing.lg))
        SubmitButton(
            enabled = rating > 0 && !isSubmitting,
            isSubmitting = isSubmitting,
            onSubmit = onSubmit,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun StarRatingSelector(rating: Int, onRatingChange: (Int) -> Unit) {
    Text("Rating", style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(Spacing.xs))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        for (i in 1..MAX_STARS) {
            IconButton(
                onClick = { onRatingChange(i) },
                modifier = Modifier.size(STAR_BUTTON_SIZE),
            ) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = stringResource(R.string.cd_stars, i),
                    tint = if (i <= rating) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    modifier = Modifier.size(STAR_ICON_SIZE),
                )
            }
        }
    }
}

@Composable
private fun RecommendationSelector(recommended: Boolean, onChanged: (Boolean) -> Unit) {
    Text("Recommendation", style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(Spacing.xs))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FilterChip(
            selected = recommended,
            onClick = { onChanged(true) },
            label = { Text("Recommend") },
            leadingIcon = { Icon(Icons.Outlined.ThumbUp, null, Modifier.size(16.dp)) },
        )
        FilterChip(
            selected = !recommended,
            onClick = { onChanged(false) },
            label = { Text("Not recommended") },
            leadingIcon = { Icon(Icons.Outlined.ThumbDown, null, Modifier.size(16.dp)) },
        )
    }
}

@Composable
private fun SubmitButton(
    enabled: Boolean,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(onClick = onSubmit, enabled = enabled, modifier = modifier) {
        if (isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
            Text("Submit")
        }
    }
}

private const val MAX_STARS = 5
private val STAR_BUTTON_SIZE = 40.dp
private val STAR_ICON_SIZE = 28.dp
private const val MIN_TEXT_LINES = 3
private const val MAX_TEXT_LINES = 6
