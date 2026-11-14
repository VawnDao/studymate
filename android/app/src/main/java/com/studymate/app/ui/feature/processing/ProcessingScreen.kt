package com.studymate.app.ui.feature.processing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studymate.app.R
import com.studymate.app.ui.adaptive.ContentWidth
import com.studymate.app.ui.adaptive.centeredContent
import com.studymate.app.ui.common.ObserveAsEvents
import com.studymate.app.ui.common.messageRes
import com.studymate.app.ui.components.AnimatedProgressBar
import com.studymate.app.ui.components.BackHeader
import com.studymate.app.ui.components.MessageView
import com.studymate.app.ui.components.staggerIn
import com.studymate.app.ui.theme.StudyTheme

@Composable
fun ProcessingRoute(
    onBack: () -> Unit,
    onCompleted: (documentId: String) -> Unit,
    viewModel: ProcessingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is ProcessingEffect.Completed -> onCompleted(effect.documentId)
        }
    }
    ProcessingScreen(state, onBack, viewModel::retry)
}

private enum class StepState { WAIT, RUN, DONE }

@Composable
fun ProcessingScreen(state: ProcessingUiState, onBack: () -> Unit, onRetry: () -> Unit) {
    val steps = listOf(
        R.string.processing_step_read to R.string.processing_step_read_sub,
        R.string.processing_step_ocr to R.string.processing_step_ocr_sub,
        R.string.processing_step_index to R.string.processing_step_index_sub,
        R.string.processing_step_generate to R.string.processing_step_generate_sub,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .centeredContent(ContentWidth.Focus)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BackHeader(title = stringResource(R.string.processing_header), onBack = onBack)
        Column(Modifier.staggerIn(0)) {
            Text(stringResource(R.string.processing_label), style = MaterialTheme.typography.labelMedium, color = StudyTheme.colors.primary)
            Text(state.title ?: stringResource(R.string.processing_untitled), style = MaterialTheme.typography.headlineSmall, maxLines = 2)
        }
        AnimatedProgressBar(progress = state.progress, color = StudyTheme.colors.primary, height = 10)
        if (!state.hasProblem) {
            var elapsedSeconds by remember { mutableIntStateOf(0) }
            LaunchedEffect(state.currentStep >= PROCESSING_STEPS) {
                if (state.currentStep >= PROCESSING_STEPS) return@LaunchedEffect
                while (true) {
                    kotlinx.coroutines.delay(1_000)
                    elapsedSeconds++
                }
            }
            val etaText = if (state.currentStep >= PROCESSING_STEPS) {
                stringResource(R.string.processing_eta_done, formatElapsed(elapsedSeconds))
            } else {
                stringResource(
                    R.string.processing_eta_running,
                    estimateRemainingText(state.currentStep, elapsedSeconds),
                    formatElapsed(elapsedSeconds),
                )
            }
            Text(
                text = etaText,
                style = MaterialTheme.typography.bodySmall,
                color = StudyTheme.colors.inkMuted,
            )
        }

        AnimatedContent(
            targetState = state.hasProblem,
            transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(160)) },
            label = "processingContent",
        ) { hasProblem ->
            if (hasProblem) {
                val body = state.failure?.messageRes() ?: state.error?.messageRes() ?: R.string.error_generic
                MessageView(
                    title = stringResource(
                        if (state.failure != null) R.string.processing_failed_title else R.string.processing_error_title,
                    ),
                    body = stringResource(body),
                    actionLabel = if (state.failure == null) stringResource(R.string.action_retry) else null,
                    onAction = if (state.failure == null) onRetry else null,
                    secondaryLabel = stringResource(R.string.action_back_home),
                    onSecondary = onBack,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    steps.forEachIndexed { index, (label, sub) ->
                        val stepState = when {
                            index < state.currentStep -> StepState.DONE
                            index == state.currentStep -> StepState.RUN
                            else -> StepState.WAIT
                        }
                        StepRow(index, stringResource(label), stringResource(sub), stepState, Modifier.staggerIn(index + 1))
                    }
                    Text(
                        stringResource(R.string.processing_footnote),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudyTheme.colors.inkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepRow(index: Int, label: String, sub: String, state: StepState, modifier: Modifier) {
    val colors = StudyTheme.colors
    val alpha by animateFloatAsState(if (state == StepState.WAIT) 0.55f else 1f, tween(350), label = "stepAlpha")
    val badge by animateColorAsState(
        when (state) {
            StepState.DONE -> colors.mint
            StepState.RUN -> colors.primary
            StepState.WAIT -> colors.background
        },
        tween(300),
        label = "stepBadge",
    )
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(state) {
        if (state == StepState.DONE) {
            bounce.animateTo(1f, keyframes { durationMillis = 450; 1.08f at 180; 1f at 450 })
        }
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "ring",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer {
                    scaleX = bounce.value
                    scaleY = bounce.value
                }
                .drawBehind {
                    if (state == StepState.RUN) {
                        val spread = 8.dp.toPx() * ring
                        drawRoundRect(
                            color = colors.primary.copy(alpha = 0.4f * (1f - ring)),
                            topLeft = androidx.compose.ui.geometry.Offset(-spread, -spread),
                            size = androidx.compose.ui.geometry.Size(size.width + spread * 2, size.height + spread * 2),
                            cornerRadius = CornerRadius(12.dp.toPx() + spread),
                        )
                    }
                }
                .clip(RoundedCornerShape(12.dp))
                .background(badge),
            contentAlignment = Alignment.Center,
        ) {
            if (state == StepState.DONE) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(18.dp))
            } else {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (state == StepState.RUN) Color.White else colors.inkMuted,
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
        val status = when (state) {
            StepState.DONE -> stringResource(R.string.processing_done)
            StepState.RUN -> stringResource(R.string.processing_running)
            StepState.WAIT -> ""
        }
        Text(status, style = MaterialTheme.typography.labelMedium, color = if (state == StepState.DONE) colors.success else colors.primary)
    }
}

private fun estimateRemainingText(step: Int, elapsedSeconds: Int): String = when (step) {
    0 -> when {
        elapsedSeconds >= 90 -> "~2–4 phút"
        elapsedSeconds >= 25 -> "~1–3 phút"
        else -> "~20–60 giây"
    }
    1 -> if (elapsedSeconds >= 45) "~1–3 phút" else "~30–90 giây"
    2 -> if (elapsedSeconds >= 45) "~30–90 giây" else "~15–35 giây"
    else -> "~10–25 giây"
}

private fun formatElapsed(seconds: Int): String = if (seconds < 60) {
    "${seconds}s"
} else {
    "${seconds / 60}m ${seconds % 60}s"
}
