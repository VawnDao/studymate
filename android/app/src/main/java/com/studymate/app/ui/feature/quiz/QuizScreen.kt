package com.studymate.app.ui.feature.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studymate.app.R
import com.studymate.app.ui.components.AnimatedProgressBar
import com.studymate.app.ui.components.CircleIconButton
import com.studymate.app.ui.components.LoadingView
import com.studymate.app.ui.components.MathText
import com.studymate.app.ui.components.MessageView
import com.studymate.app.ui.components.SourceChip
import com.studymate.app.ui.components.StudyButton
import com.studymate.app.ui.components.StudyButtonStyle
import com.studymate.app.ui.components.pressScale
import com.studymate.app.ui.feature.source.SourceRequest
import com.studymate.app.ui.feature.source.SourceSheet
import com.studymate.app.ui.theme.Motion
import com.studymate.app.ui.theme.StudyTheme
import com.studymate.domain.model.QuizItem
import com.studymate.domain.model.QuizResult
import kotlin.math.roundToInt

@Composable
fun QuizRoute(
    onClose: () -> Unit,
    onOpenTutor: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var source by remember { mutableStateOf<SourceRequest?>(null) }
    QuizScreen(
        state = state,
        onClose = onClose,
        onOpenTutor = onOpenTutor,
        onEvent = viewModel::onEvent,
        onOpenSource = { source = SourceRequest(viewModel.documentId, it) },
    )
    source?.let { SourceSheet(request = it, onDismiss = { source = null }) }
}

@Composable
fun QuizScreen(
    state: QuizUiState,
    onClose: () -> Unit,
    onOpenTutor: () -> Unit,
    onEvent: (QuizEvent) -> Unit,
    onOpenSource: (chunkId: String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircleIconButton(Icons.Filled.Close, stringResource(R.string.quiz_close), onClose)
            AnimatedProgressBar(state.progress, StudyTheme.colors.orange, Modifier.weight(1f))
            if (state.items.isNotEmpty()) {
                Text("${state.index + 1}/${state.items.size}", style = MaterialTheme.typography.titleSmall)
            }
        }
        when {
            state.isLoading -> LoadingView()
            state.items.isEmpty() -> MessageView(
                title = stringResource(R.string.quiz_empty_title),
                body = stringResource(R.string.quiz_empty_body),
                actionLabel = stringResource(R.string.action_back),
                onAction = onClose,
            )
            state.result != null -> ResultContent(state.result, onOpenTutor, onRetry = { onEvent(QuizEvent.Retry) })
            else -> AnimatedContent(
                targetState = state.index,
                transitionSpec = {
                    (slideInHorizontally(tween(420, easing = Motion.Emphasized)) { it / 8 } + fadeIn(tween(420))) togetherWith
                        fadeOut(tween(120))
                },
                label = "question",
            ) { index ->
                val item = state.items[index]
                QuestionContent(
                    number = index + 1,
                    item = item,
                    selected = if (index == state.index) state.selected else null,
                    isLast = state.isLast,
                    isSubmitting = state.isSubmitting,
                    sourcePage = item.sourceChunkIds.firstNotNullOfOrNull { state.chunkPages[it] },
                    onEvent = onEvent,
                    onOpenSource = { item.sourceChunkIds.firstOrNull()?.let(onOpenSource) },
                )
            }
        }
    }
}

@Composable
private fun QuestionContent(
    number: Int,
    item: QuizItem,
    selected: Int?,
    isLast: Boolean,
    isSubmitting: Boolean,
    sourcePage: Int?,
    onEvent: (QuizEvent) -> Unit,
    onOpenSource: () -> Unit,
) {
    val colors = StudyTheme.colors
    val answered = selected != null
    val correct = answered && item.isCorrect(selected!!)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.quiz_question_number, number), style = MaterialTheme.typography.labelMedium, color = colors.orangeText)
        MathText(item.question, style = MaterialTheme.typography.headlineSmall)
        item.options.forEachIndexed { index, option ->
            val optionState = when {
                !answered -> OptionState.IDLE
                index == item.answerIndex -> OptionState.CORRECT
                index == selected -> OptionState.WRONG
                else -> OptionState.DIMMED
            }
            OptionButton(
                letter = ('A' + index).toString(),
                text = option,
                state = optionState,
                onClick = { onEvent(QuizEvent.Select(index)) },
            )
        }
        AnimatedVisibility(
            visible = answered,
            enter = fadeIn(tween(Motion.MEDIUM)) + slideInVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { it / 4 },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (correct) colors.mint else colors.errorTint)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(if (correct) R.string.quiz_correct else R.string.quiz_wrong),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (correct) colors.success else colors.errorText,
                    )
                    if (item.explanation.isNotBlank()) {
                        MathText(item.explanation, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                    }
                    SourceChip(page = sourcePage, onClick = onOpenSource, container = colors.surface)
                }
                StudyButton(
                    text = stringResource(if (isLast) R.string.quiz_see_result else R.string.quiz_next),
                    onClick = { onEvent(QuizEvent.Next) },
                    loading = isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private enum class OptionState { IDLE, CORRECT, WRONG, DIMMED }

/** Đáp án đúng nảy nhẹ, đáp án sai rung ngang 420 ms (FE.md 10.1). */
@Composable
private fun OptionButton(letter: String, text: String, state: OptionState, onClick: () -> Unit) {
    val colors = StudyTheme.colors
    val container by animateColorAsState(
        when (state) {
            OptionState.CORRECT -> colors.mint
            OptionState.WRONG -> colors.errorTint
            else -> colors.surface
        },
        tween(Motion.MEDIUM),
        label = "optionBg",
    )
    val border by animateColorAsState(
        when (state) {
            OptionState.CORRECT -> colors.success
            OptionState.WRONG -> colors.error
            else -> colors.surface
        },
        tween(Motion.MEDIUM),
        label = "optionBorder",
    )
    val shake = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(state) {
        when (state) {
            OptionState.WRONG -> shake.animateTo(0f, keyframes {
                durationMillis = 420
                -7f at 80
                6f at 160
                -4f at 240
                3f at 320
            })
            OptionState.CORRECT -> pop.animateTo(1f, keyframes {
                durationMillis = 450
                1.035f at 180
            })
            else -> Unit
        }
    }
    val interaction = remember { MutableInteractionSource() }
    val markLabel = when (state) {
        OptionState.CORRECT -> stringResource(R.string.quiz_mark_correct)
        OptionState.WRONG -> stringResource(R.string.quiz_mark_chosen)
        else -> ""
    }
    Surface(
        onClick = onClick,
        enabled = state == OptionState.IDLE,
        shape = RoundedCornerShape(20.dp),
        color = container,
        contentColor = colors.ink,
        border = BorderStroke(2.dp, border),
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                alpha = if (state == OptionState.DIMMED) 0.55f else 1f
            }
            .pressScale(interaction)
            .semantics {
                role = Role.RadioButton
                selected = state == OptionState.WRONG || state == OptionState.CORRECT
            },
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val (letterBg, letterFg) = when (state) {
                OptionState.CORRECT -> colors.success to Color.White
                OptionState.WRONG -> colors.error to Color.White
                else -> colors.background to colors.inkSecondary
            }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(letterBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter, style = MaterialTheme.typography.titleSmall, color = letterFg)
            }
            MathText(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (markLabel.isNotEmpty()) {
                Text(
                    markLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state == OptionState.CORRECT) colors.success else colors.errorText,
                )
            }
        }
    }
}

@Composable
private fun ResultContent(result: QuizResult, onOpenTutor: () -> Unit, onRetry: () -> Unit) {
    val colors = StudyTheme.colors
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val ratio = if (result.total == 0) 0f else result.score.toFloat() / result.total
    val message = when {
        ratio >= 1f -> R.string.quiz_result_perfect
        ratio >= 0.5f -> R.string.quiz_result_good
        else -> R.string.quiz_result_low
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(initialScale = 0.86f, animationSpec = tween(550, easing = Motion.Overshoot)) + fadeIn(),
        ) {
            Surface(shape = RoundedCornerShape(28.dp), color = colors.peach) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(R.string.quiz_result_label), style = MaterialTheme.typography.labelMedium, color = colors.orangeText)
                    Text(
                        stringResource(R.string.quiz_score, result.score, result.total),
                        style = MaterialTheme.typography.displayLarge,
                    )
                    Text(
                        stringResource(R.string.quiz_percent, (ratio * 100).roundToInt()),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.inkSecondary,
                    )
                    Text(stringResource(message), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
        }
        if (result.wrongItemIds.isNotEmpty()) {
            StudyButton(stringResource(R.string.quiz_ask_tutor), onOpenTutor, Modifier.fillMaxWidth())
        }
        StudyButton(stringResource(R.string.quiz_retry), onRetry, Modifier.fillMaxWidth(), style = StudyButtonStyle.Outline)
    }
}
