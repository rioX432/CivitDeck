package com.riox432.civitdeck.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.riox432.civitdeck.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun GestureTutorialScreen(onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { tutorialSteps.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SkipButton(onDismiss)
        Spacer(modifier = Modifier.weight(1f))
        TutorialPager(pagerState)
        Spacer(modifier = Modifier.height(Spacing.xl))
        PageIndicator(pagerState)
        Spacer(modifier = Modifier.height(Spacing.xl))
        NavigationButtons(
            pagerState = pagerState,
            onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
            onDismiss = onDismiss,
        )
        Spacer(modifier = Modifier.weight(0.5f))
    }
}

@Composable
private fun SkipButton(onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        TextButton(onClick = onDismiss) {
            Text("Skip")
        }
    }
}

@Composable
private fun TutorialPager(pagerState: PagerState) {
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
        TutorialPageContent(tutorialSteps[page])
    }
}

@Composable
private fun TutorialPageContent(step: TutorialStep) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepAnimation(step)
        Spacer(modifier = Modifier.height(Spacing.xxl))
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.xl),
        )
    }
}

@Composable
private fun StepAnimation(step: TutorialStep) {
    when (tutorialSteps.indexOf(step)) {
        0 -> SwipeDiscoveryAnimation(accentColor = step.accentColor)
        1 -> QuickActionsAnimation(accentColor = step.accentColor)
        2 -> ImageComparisonAnimation(accentColor = step.accentColor)
    }
}

@Composable
private fun PageIndicator(pagerState: PagerState) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        repeat(pagerState.pageCount) { index ->
            val color = if (index == pagerState.currentPage) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
            Box(
                modifier = Modifier
                    .size(Spacing.sm)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun NavigationButtons(
    pagerState: PagerState,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isLastPage = pagerState.currentPage == pagerState.pageCount - 1
    Button(
        onClick = if (isLastPage) onDismiss else onNext,
        modifier = Modifier.fillMaxWidth(0.6f),
    ) {
        Text(if (isLastPage) "Get Started" else "Next")
    }
}
