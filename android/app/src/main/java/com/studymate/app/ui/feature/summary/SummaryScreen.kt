package com.studymate.app.ui.feature.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studymate.app.R
import com.studymate.app.ui.adaptive.AdaptiveWidth
import com.studymate.app.ui.adaptive.ContentWidth
import com.studymate.app.ui.adaptive.centeredContent
import com.studymate.app.ui.components.BackHeader
import com.studymate.app.ui.components.LoadingView
import com.studymate.app.ui.components.MathText
import com.studymate.app.ui.components.MessageView
import com.studymate.app.ui.components.SectionTitle
import com.studymate.app.ui.components.SourceChip
import com.studymate.app.ui.components.StudyButton
import com.studymate.app.ui.components.staggerIn
import com.studymate.app.ui.feature.source.SourceRequest
import com.studymate.app.ui.feature.source.SourceSheet
import com.studymate.app.ui.theme.Motion
import com.studymate.app.ui.theme.StudyTheme
import com.studymate.domain.model.Concept
import com.studymate.domain.model.OutlineSection
import com.studymate.domain.model.StudyPack

@Composable
fun SummaryRoute(
    onBack: () -> Unit,
    onOpenCards: () -> Unit,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var source by remember { mutableStateOf<SourceRequest?>(null) }
    SummaryScreen(
        state = state,
        onBack = onBack,
        onToggle = viewModel::toggleSection,
        onOpenCards = onOpenCards,
        onOpenSource = { source = SourceRequest(viewModel.documentId, it) },
    )
    source?.let { SourceSheet(request = it, onDismiss = { source = null }) }
}

@Composable
fun SummaryScreen(
    state: SummaryUiState,
    onBack: () -> Unit,
    onToggle: (Int) -> Unit,
    onOpenCards: () -> Unit,
    onOpenSource: (chunkId: String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .centeredContent(if (AdaptiveWidth.current.isExpanded) ContentWidth.Wide else ContentWidth.Reading)
            .systemBarsPadding(),
    ) {
        BackHeader(
            title = stringResource(R.string.summary_title),
            onBack = onBack,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        when {
            state.isLoading -> LoadingView()
            state.pack == null -> MessageView(
                title = stringResource(R.string.pack_missing_title),
                body = stringResource(R.string.pack_missing_body),
                actionLabel = stringResource(R.string.action_back),
                onAction = onBack,
            )
            else -> SummaryContent(state.pack, state.expanded, onToggle, onOpenCards, onOpenSource)
        }
    }
}

@Composable
private fun SummaryContent(
    pack: StudyPack,
    expanded: Set<Int>,
    onToggle: (Int) -> Unit,
    onOpenCards: () -> Unit,
    onOpenSource: (chunkId: String) -> Unit,
) {
    if (AdaptiveWidth.current.isExpanded) {
        SummaryTwoPane(pack, expanded, onToggle, onOpenCards, onOpenSource)
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "outline-title") { SectionTitle(stringResource(R.string.summary_outline)) }
        itemsIndexed(pack.summary.outline, key = { index, _ -> "outline-$index" }) { index, section ->
            OutlineCard(
                index = index,
                section = section,
                sourcePage = pack.firstSourceOf(section.sourceChunkIds)?.page,
                expanded = index in expanded,
                onToggle = { onToggle(index) },
                onOpenSource = { pack.firstSourceOf(section.sourceChunkIds)?.let { onOpenSource(it.chunkId) } },
                modifier = Modifier.staggerIn(index),
            )
        }
        if (pack.summary.concepts.isNotEmpty()) {
            item(key = "concepts-title") {
                SectionTitle(stringResource(R.string.summary_concepts), Modifier.padding(top = 8.dp))
            }
            itemsIndexed(pack.summary.concepts, key = { index, _ -> "concept-$index" }) { index, concept ->
                ConceptCard(pack, concept, onOpenSource, Modifier.staggerIn(index + pack.summary.outline.size))
            }
        }
        item(key = "cta") {
            StudyButton(
                text = stringResource(R.string.summary_to_cards),
                onClick = onOpenCards,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun OutlineCard(
    index: Int,
    section: OutlineSection,
    sourcePage: Int?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenSource: () -> Unit,
    modifier: Modifier,
) {
    val colors = StudyTheme.colors
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(Motion.MEDIUM, easing = Motion.Emphasized), label = "chevron")
    val stateLabel = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = stateLabel }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primaryTint),
                contentAlignment = Alignment.Center,
            ) {
                Text("${index + 1}", style = MaterialTheme.typography.titleSmall, color = colors.primaryStrong)
            }
            Text(section.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = colors.inkMuted,
                modifier = Modifier.rotate(rotation),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)) + fadeIn(),
            exit = shrinkVertically(tween(Motion.SHORT)) + fadeOut(tween(Motion.SHORT)),
        ) {
            Column(
                Modifier.padding(start = 58.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                section.points.forEach { point ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", style = MaterialTheme.typography.titleSmall, color = colors.primary)
                        MathText(point, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (sourcePage != null) SourceChip(page = sourcePage, onClick = onOpenSource)
            }
        }
    }
}

/** Tablet ngang: dàn ý bên trái, khái niệm bên phải, mỗi cột cuộn riêng. */
@Composable
private fun SummaryTwoPane(
    pack: StudyPack,
    expanded: Set<Int>,
    onToggle: (Int) -> Unit,
    onOpenCards: () -> Unit,
    onOpenSource: (chunkId: String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1.15f),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "outline-title") { SectionTitle(stringResource(R.string.summary_outline)) }
            itemsIndexed(pack.summary.outline, key = { index, _ -> "outline-$index" }) { index, section ->
                OutlineCard(
                    index = index,
                    section = section,
                    sourcePage = pack.firstSourceOf(section.sourceChunkIds)?.page,
                    expanded = index in expanded,
                    onToggle = { onToggle(index) },
                    onOpenSource = { pack.firstSourceOf(section.sourceChunkIds)?.let { onOpenSource(it.chunkId) } },
                    modifier = Modifier.staggerIn(index),
                )
            }
            item(key = "cta") {
                StudyButton(
                    text = stringResource(R.string.summary_to_cards),
                    onClick = onOpenCards,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "concepts-title") { SectionTitle(stringResource(R.string.summary_concepts)) }
            itemsIndexed(pack.summary.concepts, key = { index, _ -> "concept-$index" }) { index, concept ->
                ConceptCard(pack, concept, onOpenSource, Modifier.staggerIn(index + 1))
            }
        }
    }
}

@Composable
private fun ConceptCard(pack: StudyPack, concept: Concept, onOpenSource: (chunkId: String) -> Unit, modifier: Modifier) {
    val source = pack.firstSourceOf(concept.sourceChunkIds)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StudyTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(concept.term, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            source?.let { SourceChip(page = it.page, onClick = { onOpenSource(it.chunkId) }) }
        }
        MathText(concept.explain, style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.inkSecondary)
    }
}
