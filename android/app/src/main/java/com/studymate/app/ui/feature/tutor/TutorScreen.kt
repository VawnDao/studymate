package com.studymate.app.ui.feature.tutor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studymate.app.R
import com.studymate.app.ui.common.messageRes
import com.studymate.app.ui.components.BackHeader
import com.studymate.app.ui.components.MathText
import com.studymate.app.ui.components.SourceChip
import com.studymate.app.ui.components.Tag
import com.studymate.app.ui.components.TypingIndicator
import com.studymate.app.ui.feature.source.SourceRequest
import com.studymate.app.ui.feature.source.SourceSheet
import com.studymate.app.ui.theme.Motion
import com.studymate.app.ui.theme.StudyTheme
import com.studymate.domain.model.Citation
import com.studymate.domain.model.TutorMode
import com.studymate.domain.model.TutorRole

@Composable
fun TutorRoute(
    onBack: () -> Unit,
    viewModel: TutorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var source by remember { mutableStateOf<SourceRequest?>(null) }
    TutorScreen(
        state = state,
        onBack = onBack,
        onEvent = viewModel::onEvent,
        onOpenSource = { source = SourceRequest(viewModel.documentId, it.chunkId, it.quote) },
    )
    source?.let { SourceSheet(request = it, onDismiss = { source = null }) }
}

@Composable
fun TutorScreen(
    state: TutorUiState,
    onBack: () -> Unit,
    onEvent: (TutorEvent) -> Unit,
    onOpenSource: (Citation) -> Unit,
) {
    val listState = rememberLazyListState()
    // Khoá theo cả số tin lẫn trạng thái gửi: câu trả lời thay chỗ dấu "đang gõ" nên tổng số
    // phần tử có thể không đổi, nhưng vẫn phải cuộn tới tin mới.
    val itemCount = state.items.size + 1 + (if (state.isSending) 1 else 0)
    LaunchedEffect(state.items.size, state.isSending) { listState.animateScrollToItem(itemCount - 1) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        BackHeader(
            title = stringResource(R.string.tutor_title),
            subtitle = if (state.title.isNotBlank()) stringResource(R.string.tutor_subtitle, state.title) else null,
            onBack = onBack,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "greeting") {
                AssistantBubble(
                    text = stringResource(R.string.tutor_greeting),
                    mode = null,
                    citations = emptyList(),
                    nextQuestion = null,
                    onOpenSource = onOpenSource,
                )
            }
            items(state.items, key = { it.id }) { item ->
                Box(Modifier.animateItem()) {
                    BubbleEntrance(fromEnd = item.role == TutorRole.USER) {
                        if (item.role == TutorRole.USER) {
                            UserBubble(item.text)
                        } else {
                            AssistantBubble(item.text, item.mode, item.citations, item.nextQuestion, onOpenSource)
                        }
                    }
                }
            }
            if (state.isSending) {
                item(key = "typing") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AiAvatar()
                        Surface(shape = RoundedCornerShape(20.dp), color = StudyTheme.colors.surface) {
                            TypingIndicator(Modifier.padding(horizontal = 16.dp, vertical = 14.dp))
                        }
                        Text(stringResource(R.string.tutor_searching), style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.inkMuted)
                    }
                }
            }
        }
        Composer(state, onEvent)
    }
}

/** Bong bóng mới nảy từ góc dưới: trái cho AI, phải cho người dùng (FE.md 10.1). */
@Composable
private fun BubbleEntrance(fromEnd: Boolean, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(Motion.MEDIUM)) + scaleIn(
            initialScale = 0.94f,
            transformOrigin = TransformOrigin(if (fromEnd) 1f else 0f, 1f),
            animationSpec = tween(Motion.MEDIUM, easing = Motion.Emphasized),
        ),
    ) { content() }
}

@Composable
private fun AiAvatar() {
    Box(
        Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(StudyTheme.colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.tutor_ai_badge), style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun UserBubble(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                .background(StudyTheme.colors.primary)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantBubble(
    text: String,
    mode: TutorMode?,
    citations: List<Citation>,
    nextQuestion: String?,
    onOpenSource: (Citation) -> Unit,
) {
    val colors = StudyTheme.colors
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AiAvatar()
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
                .background(colors.surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            mode?.let { ModeTag(it) }
            MathText(text, style = MaterialTheme.typography.bodyMedium)
            nextQuestion?.let {
                MathText(it, style = MaterialTheme.typography.titleSmall, color = colors.primaryStrong)
            }
            if (citations.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    citations.distinctBy { it.chunkId }.forEach { citation ->
                        SourceChip(page = citation.page, onClick = { onOpenSource(citation) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeTag(mode: TutorMode) {
    val colors = StudyTheme.colors
    val (label, container, content) = when (mode) {
        TutorMode.HINT -> Triple(R.string.tutor_mode_hint, colors.primaryTint, colors.primaryStrong)
        TutorMode.CHECK -> Triple(R.string.tutor_mode_check, colors.warningTint, colors.warningText)
        TutorMode.SOLUTION -> Triple(R.string.tutor_mode_solution, colors.ink, Color.White)
        TutorMode.OUT_OF_SCOPE -> Triple(R.string.tutor_mode_out_of_scope, colors.errorTint, colors.errorText)
    }
    Tag(stringResource(label), container, content)
}

@Composable
private fun Composer(state: TutorUiState, onEvent: (TutorEvent) -> Unit) {
    val colors = StudyTheme.colors
    val moreHintPrompt = stringResource(R.string.tutor_more_hint_prompt)
    val solutionPrompt = stringResource(R.string.tutor_solution_prompt)
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(
            visible = state.error != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.errorTint)
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state.error?.let { stringResource(it.messageRes()) }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.errorText,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onEvent(TutorEvent.Retry) }) { Text(stringResource(R.string.action_retry)) }
            }
        }
        AnimatedVisibility(
            visible = state.canAskForMore,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut(tween(Motion.SHORT)),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(stringResource(R.string.tutor_more_hint), dark = false) { onEvent(TutorEvent.MoreHint(moreHintPrompt)) }
                SuggestionChip(stringResource(R.string.tutor_show_solution), dark = true) { onEvent(TutorEvent.ShowSolution(solutionPrompt)) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.input,
                onValueChange = { onEvent(TutorEvent.InputChanged(it)) },
                placeholder = { Text(stringResource(R.string.tutor_input_hint)) },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (state.canSend) onEvent(TutorEvent.Send) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
            )
            FilledIconButton(
                onClick = { onEvent(TutorEvent.Send) },
                enabled = state.canSend,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.primary),
                modifier = Modifier.size(52.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.tutor_send))
            }
        }
    }
}

@Composable
private fun SuggestionChip(label: String, dark: Boolean, onClick: () -> Unit) {
    val colors = StudyTheme.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (dark) colors.ink else colors.surface,
        contentColor = if (dark) Color.White else colors.ink,
        modifier = Modifier.heightIn(min = 40.dp),
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
