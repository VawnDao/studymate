package com.studymate.app.ui.feature.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studymate.app.R
import com.studymate.app.ui.adaptive.AdaptiveWidth
import com.studymate.app.ui.adaptive.ContentWidth
import com.studymate.app.ui.adaptive.centeredContent
import com.studymate.app.ui.common.formatInterval
import com.studymate.app.ui.components.AnimatedProgressBar
import com.studymate.app.ui.components.CircleIconButton
import com.studymate.app.ui.components.LoadingView
import com.studymate.app.ui.components.MathText
import com.studymate.app.ui.components.MessageView
import com.studymate.app.ui.components.SourceChip
import com.studymate.app.ui.components.StudyButton
import com.studymate.app.ui.components.StudyButtonStyle
import com.studymate.app.ui.components.Tag
import com.studymate.app.ui.components.ShortcutHint
import com.studymate.app.ui.components.choiceIndex
import com.studymate.app.ui.components.isConfirm
import com.studymate.app.ui.components.keyboardShortcuts
import com.studymate.app.ui.components.pressScale
import com.studymate.app.ui.feature.source.SourceRequest
import com.studymate.app.ui.feature.source.SourceSheet
import com.studymate.app.ui.theme.Motion
import com.studymate.app.ui.theme.StudyTheme
import com.studymate.domain.model.Flashcard
import com.studymate.domain.model.ReviewRating

@Composable
fun CardsRoute(
    onClose: () -> Unit,
    onOpenQuiz: (documentId: String) -> Unit,
    viewModel: CardsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var source by remember { mutableStateOf<SourceRequest?>(null) }
    CardsScreen(
        state = state,
        canOpenQuiz = viewModel.documentId != null,
        onClose = onClose,
        onOpenQuiz = { viewModel.documentId?.let(onOpenQuiz) },
        onEvent = viewModel::onEvent,
        onOpenSource = { card, chunkId -> source = SourceRequest(card.documentId, chunkId) },
    )
    source?.let { SourceSheet(request = it, onDismiss = { source = null }) }
}

@Composable
fun CardsScreen(
    state: CardsUiState,
    canOpenQuiz: Boolean,
    onClose: () -> Unit,
    onOpenQuiz: () -> Unit,
    onEvent: (CardsEvent) -> Unit,
    onOpenSource: (Flashcard, String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .centeredContent(ContentWidth.Focus)
            .keyboardShortcuts { key -> handleCardsKey(state, key, onEvent) }
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircleIconButton(Icons.Filled.Close, stringResource(R.string.cards_close), onClose)
            AnimatedProgressBar(state.progress, StudyTheme.colors.success, Modifier.weight(1f))
            if (state.cards.isNotEmpty()) {
                Text("${minOf(state.index + 1, state.cards.size)}/${state.cards.size}", style = MaterialTheme.typography.titleSmall)
            }
        }
        when {
            state.isLoading -> LoadingView()
            state.cards.isEmpty() -> MessageView(
                title = stringResource(R.string.cards_empty_title),
                body = stringResource(R.string.cards_empty_body),
                actionLabel = stringResource(R.string.action_back),
                onAction = onClose,
            )
            state.isFinished -> FinishedContent(state, canOpenQuiz, onOpenQuiz, onClose)
            else -> ReviewContent(state, onEvent, onOpenSource)
        }
    }
}

@Composable
private fun ColumnScope.ReviewContent(
    state: CardsUiState,
    onEvent: (CardsEvent) -> Unit,
    onOpenSource: (Flashcard, String) -> Unit,
) {
    if (state.isPractice) {
        Tag(stringResource(R.string.cards_practice), StudyTheme.colors.warningTint, StudyTheme.colors.warningText)
    }
    AnimatedContent(
        targetState = state.index,
        transitionSpec = {
            (slideInHorizontally(tween(420, easing = Motion.Emphasized)) { it / 8 } + fadeIn(tween(420))) togetherWith
                fadeOut(tween(120))
        },
        modifier = Modifier.weight(1f),
        label = "card",
    ) { index ->
        val card = state.cards[index]
        FlipCard(
            card = card,
            flipped = state.isFlipped && index == state.index,
            sourcePage = card.sourceChunkIds.firstNotNullOfOrNull { state.chunkPages[it] },
            onFlip = { onEvent(CardsEvent.Flip) },
            onOpenSource = { card.sourceChunkIds.firstOrNull()?.let { onOpenSource(card, it) } },
        )
    }
    AnimatedContent(
        targetState = state.isFlipped,
        transitionSpec = { (fadeIn() + slideInVertically { it / 3 }) togetherWith fadeOut(tween(100)) },
        label = "actions",
    ) { flipped ->
        if (flipped) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.cards_rate_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = StudyTheme.colors.inkMuted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReviewRating.entries.forEach { rating ->
                        RatingButton(
                            rating = rating,
                            interval = state.previews[rating]?.let { formatInterval(it) }.orEmpty(),
                            enabled = !state.isRating,
                            onClick = { onEvent(CardsEvent.Rate(rating)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                ShortcutHint(stringResource(R.string.shortcut_cards_back), Modifier.fillMaxWidth())
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StudyButton(
                text = stringResource(R.string.cards_show_answer),
                onClick = { onEvent(CardsEvent.Flip) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            )
            ShortcutHint(stringResource(R.string.shortcut_cards_front), Modifier.fillMaxWidth())
            }
        }
    }
}

/** Thẻ lật 3D (FE.md 10.1): xoay Y 180°, 500 ms; mặt sau xoay thêm 180° để chữ không bị ngược. */
@Composable
private fun FlipCard(
    card: Flashcard,
    flipped: Boolean,
    sourcePage: Int?,
    onFlip: () -> Unit,
    onOpenSource: () -> Unit,
) {
    val colors = StudyTheme.colors
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(500, easing = Motion.Emphasized),
        label = "flip",
    )
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    val flipLabel = stringResource(R.string.cards_flip)
    val showBack = rotation > 90f
    // Tablet: thẻ lớn hơn nên chữ cũng lớn hơn để dễ đọc từ xa.
    val large = !AdaptiveWidth.current.isCompact
    val frontStyle = if (large) MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp, lineHeight = 44.sp) else MaterialTheme.typography.headlineMedium
    val backStyle = if (large) MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 40.sp) else MaterialTheme.typography.headlineSmall
    Surface(
        onClick = onFlip,
        interactionSource = interaction,
        shape = RoundedCornerShape(32.dp),
        color = if (showBack) colors.ink else colors.surface,
        contentColor = if (showBack) Color.White else colors.ink,
        modifier = Modifier
            .fillMaxSize()
            .pressScale(interaction, 0.98f)
            .semantics { stateDescription = flipLabel }
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density.density
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationY = if (showBack) 180f else 0f }
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (showBack) {
                Text(stringResource(R.string.cards_answer), style = MaterialTheme.typography.labelSmall, color = Color(0xFFB8F5D0))
                MathText(card.back, style = backStyle)
                SourceChip(page = sourcePage, onClick = onOpenSource, container = Color.White.copy(alpha = 0.16f), content = Color.White)
            } else {
                Text(stringResource(R.string.cards_question), style = MaterialTheme.typography.labelSmall, color = colors.success)
                MathText(card.front, style = frontStyle)
                Text(stringResource(R.string.cards_tap_to_flip), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
        }
    }
}

@Composable
private fun RatingButton(rating: ReviewRating, interval: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = StudyTheme.colors
    val (container, content, label) = when (rating) {
        ReviewRating.AGAIN -> Triple(colors.errorTint, colors.errorText, R.string.rating_again)
        ReviewRating.HARD -> Triple(colors.warningTint, colors.warningText, R.string.rating_hard)
        ReviewRating.GOOD -> Triple(colors.primaryTint, colors.primaryStrong, R.string.rating_good)
        ReviewRating.EASY -> Triple(colors.mint, colors.success, R.string.rating_easy)
    }
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = content,
        interactionSource = interaction,
        modifier = modifier
            .height(64.dp)
            .pressScale(interaction)
            .semantics { this.role = Role.Button },
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
            Text(interval, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ColumnScope.FinishedContent(state: CardsUiState, canOpenQuiz: Boolean, onOpenQuiz: () -> Unit, onClose: () -> Unit) {
    val colors = StudyTheme.colors
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.86f, animationSpec = tween(550, easing = Motion.Overshoot)) + fadeIn(),
    ) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.success, contentColor = Color.White) {
            Column(Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.cards_done_label), style = MaterialTheme.typography.labelMedium)
                Text(
                    stringResource(R.string.cards_done_title, state.remembered, state.cards.size),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(stringResource(R.string.cards_done_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    Surface(shape = RoundedCornerShape(22.dp), color = colors.surface) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.cards_breakdown), style = MaterialTheme.typography.titleSmall)
            ReviewRating.entries.forEach { rating ->
                Row {
                    Text(stringResource(rating.labelRes()), style = MaterialTheme.typography.bodyMedium, color = colors.inkSecondary, modifier = Modifier.weight(1f))
                    Text("${state.ratingCounts[rating] ?: 0}", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
    Spacer(Modifier.weight(1f))
    if (canOpenQuiz) {
        StudyButton(stringResource(R.string.cards_to_quiz), onOpenQuiz, Modifier.fillMaxWidth())
    }
    StudyButton(stringResource(R.string.cards_finish), onClose, Modifier.fillMaxWidth(), style = StudyButtonStyle.Outline)
    Box(Modifier.height(4.dp))
}

private fun ReviewRating.labelRes(): Int = when (this) {
    ReviewRating.AGAIN -> R.string.rating_again
    ReviewRating.HARD -> R.string.rating_hard
    ReviewRating.GOOD -> R.string.rating_good
    ReviewRating.EASY -> R.string.rating_easy
}

/** Space/Enter lật thẻ; sau khi lật: 1 Quên · 2 Khó · 3 Nhớ · 4 Dễ. */
private fun handleCardsKey(state: CardsUiState, key: Key, onEvent: (CardsEvent) -> Unit): Boolean {
    if (state.isFinished || state.current == null || state.isRating) return false
    if (!state.isFlipped) {
        if (!key.isConfirm) return false
        onEvent(CardsEvent.Flip)
        return true
    }
    val rating = key.choiceIndex()?.let { ReviewRating.entries.getOrNull(it) } ?: return false
    onEvent(CardsEvent.Rate(rating))
    return true
}
