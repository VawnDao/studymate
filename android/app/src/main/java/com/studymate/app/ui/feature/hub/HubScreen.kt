package com.studymate.app.ui.feature.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studymate.app.R
import com.studymate.app.ui.adaptive.AdaptiveWidth
import com.studymate.app.ui.adaptive.ContentWidth
import com.studymate.app.ui.adaptive.centeredContent
import com.studymate.app.ui.common.messageRes
import com.studymate.app.ui.components.BackHeader
import com.studymate.app.ui.components.LoadingView
import com.studymate.app.ui.components.MathText
import com.studymate.app.ui.components.MessageView
import com.studymate.app.ui.components.PressableCard
import com.studymate.app.ui.components.SourceChip
import com.studymate.app.ui.components.Tag
import com.studymate.app.ui.components.staggerIn
import com.studymate.app.ui.feature.source.SourceRequest
import com.studymate.app.ui.feature.source.SourceSheet
import com.studymate.app.ui.theme.StudyTheme
import com.studymate.domain.model.DocumentHub

@Composable
fun HubRoute(
    onBack: () -> Unit,
    onOpenSummary: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenTutor: () -> Unit,
    viewModel: HubViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var source by remember { mutableStateOf<SourceRequest?>(null) }
    HubScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onOpenSummary = onOpenSummary,
        onOpenCards = onOpenCards,
        onOpenQuiz = onOpenQuiz,
        onOpenTutor = onOpenTutor,
        onOpenSource = { chunkId -> source = SourceRequest(viewModel.documentId, chunkId) },
    )
    source?.let { SourceSheet(request = it, onDismiss = { source = null }) }
}

@Composable
fun HubScreen(
    state: HubUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenSummary: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenTutor: () -> Unit,
    onOpenSource: (chunkId: String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .centeredContent(ContentWidth.Wide)
            .systemBarsPadding(),
    ) {
        BackHeader(
            title = "",
            onBack = onBack,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            trailing = if (state.hub != null) {
                { Tag(stringResource(R.string.hub_ready), StudyTheme.colors.mint, StudyTheme.colors.success) }
            } else {
                null
            },
        )
        when {
            state.hub != null -> HubContent(state.hub, onOpenSummary, onOpenCards, onOpenQuiz, onOpenTutor, onOpenSource)
            state.error != null -> MessageView(
                title = stringResource(R.string.hub_error_title),
                body = stringResource(state.error.messageRes()),
                actionLabel = stringResource(R.string.action_retry),
                onAction = onRetry,
            )
            else -> LoadingView()
        }
    }
}

@Composable
private fun HubContent(
    hub: DocumentHub,
    onOpenSummary: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenTutor: () -> Unit,
    onOpenSource: (chunkId: String) -> Unit,
) {
    val pack = hub.pack
    val colors = StudyTheme.colors
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(Modifier.staggerIn(0)) {
            Text(pack.title, style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(
                    R.string.hub_meta,
                    pluralStringResource(R.plurals.pages, pack.pages, pack.pages),
                    pluralStringResource(R.plurals.cards, pack.flashcards.size, pack.flashcards.size),
                    pluralStringResource(R.plurals.questions, pack.quiz.size, pack.quiz.size),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
            )
        }
        if (pack.summary.tldr.isNotBlank()) {
            Surface(shape = RoundedCornerShape(22.dp), color = colors.surface, modifier = Modifier.staggerIn(1)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.hub_tldr), style = MaterialTheme.typography.labelSmall, color = colors.primary)
                    MathText(pack.summary.tldr, style = MaterialTheme.typography.bodyMedium)
                    val firstSource = pack.summary.outline.firstOrNull()?.sourceChunkIds?.let(pack::firstSourceOf)
                    firstSource?.let { SourceChip(page = it.page, onClick = { onOpenSource(it.chunkId) }) }
                }
            }
        }
        val tiles: List<@Composable (Modifier) -> Unit> = listOf(
            { modifier ->
                HubTile(
                    title = stringResource(R.string.hub_summary),
                    subtitle = stringResource(R.string.hub_summary_sub, pack.summary.outline.size, pack.summary.concepts.size),
                    icon = Icons.Outlined.Lightbulb,
                    container = colors.primaryTint,
                    tint = colors.primaryStrong,
                    onClick = onOpenSummary,
                    modifier = modifier,
                )
            },
            { modifier ->
                HubTile(
                    title = stringResource(R.string.hub_cards),
                    subtitle = stringResource(
                        R.string.hub_cards_sub,
                        pluralStringResource(R.plurals.cards, pack.flashcards.size, pack.flashcards.size),
                        hub.dueCount,
                    ),
                    icon = Icons.Outlined.Style,
                    container = colors.mint,
                    tint = colors.success,
                    onClick = onOpenCards,
                    modifier = modifier,
                )
            },
            { modifier ->
                HubTile(
                    title = stringResource(R.string.hub_quiz),
                    subtitle = pluralStringResource(R.plurals.questions_with_explain, pack.quiz.size, pack.quiz.size),
                    icon = Icons.Outlined.Quiz,
                    container = colors.peach,
                    tint = colors.orangeText,
                    onClick = onOpenQuiz,
                    modifier = modifier,
                )
            },
            { modifier ->
                HubTile(
                    title = stringResource(R.string.hub_tutor),
                    subtitle = stringResource(R.string.hub_tutor_sub),
                    icon = Icons.Outlined.Forum,
                    container = colors.sky,
                    tint = colors.blueText,
                    onClick = onOpenTutor,
                    modifier = modifier,
                )
            },
        )
        // Tablet ngang: 4 ô một hàng; điện thoại và tablet dọc: lưới 2 × 2.
        val perRow = if (AdaptiveWidth.current.isExpanded) 4 else 2
        tiles.chunked(perRow).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { index, tile ->
                    tile(
                        Modifier
                            .weight(1f)
                            .staggerIn(2 + rowIndex * perRow + index),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .staggerIn(6),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(18.dp), color = colors.lemon, contentColor = colors.onLemon) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.hub_trust), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun HubTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    container: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    PressableCard(onClick = onClick, color = container, modifier = modifier.height(150.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.inkSecondary, maxLines = 2)
    }
}
