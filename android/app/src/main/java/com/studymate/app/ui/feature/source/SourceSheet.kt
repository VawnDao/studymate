package com.studymate.app.ui.feature.source

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studymate.app.R
import com.studymate.app.ui.common.messageRes
import com.studymate.app.ui.components.LoadingView
import com.studymate.app.ui.components.MessageView
import com.studymate.app.ui.components.StudyButton
import com.studymate.app.ui.components.Tag
import com.studymate.app.ui.theme.Motion
import com.studymate.app.ui.theme.StudyTheme
import com.studymate.app.ui.text.LatexToUnicode
import kotlinx.coroutines.delay

/** Yêu cầu mở đoạn nguồn từ chip "Nguồn · tr. N". */
data class SourceRequest(
    val documentId: String,
    val chunkId: String,
    /** Câu trích để tô sáng; null thì tô cả đoạn. */
    val quote: String? = null,
)

/** Điện thoại / tablet dọc: đoạn nguồn trong bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSheet(
    request: SourceRequest,
    onDismiss: () -> Unit,
    viewModel: SourceViewModel = hiltViewModel(key = sourceKey(request)),
) {
    LaunchedEffect(request) { viewModel.load(request.documentId, request.chunkId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StudyTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SourceHeader(state, onClose = null)
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                SourceBody(state, request.quote, onRetry = { viewModel.load(request.documentId, request.chunkId, force = true) })
            }
            StudyButton(text = stringResource(R.string.action_understood), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * Tablet ngang: panel nguồn cố định bên cạnh nội dung học, để vừa trả lời vừa đối chiếu tài liệu.
 * [request] null → hiện hướng dẫn "chạm Nguồn để xem ở đây".
 */
@Composable
fun SourcePanel(
    request: SourceRequest?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        color = StudyTheme.colors.surface,
    ) {
        AnimatedContent(
            targetState = request,
            transitionSpec = {
                (fadeIn(tween(Motion.MEDIUM)) + slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { it / 10 }) togetherWith
                    fadeOut(tween(Motion.SHORT))
            },
            contentKey = { it?.let(::sourceKey) },
            label = "sourcePanel",
        ) { current ->
            if (current == null) {
                SourcePanelPlaceholder()
            } else {
                val viewModel: SourceViewModel = hiltViewModel(key = sourceKey(current))
                LaunchedEffect(current) { viewModel.load(current.documentId, current.chunkId) }
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SourceHeader(state, onClose = onClose)
                    SourceBody(state, current.quote, onRetry = { viewModel.load(current.documentId, current.chunkId, force = true) })
                }
            }
        }
    }
}

@Composable
private fun SourcePanelPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(StudyTheme.colors.primaryTint),
            contentAlignment = Alignment.Center,
        ) {
            @Suppress("DEPRECATION")
            Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = StudyTheme.colors.primaryStrong)
        }
        Text(
            stringResource(R.string.source_panel_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            stringResource(R.string.source_panel_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = StudyTheme.colors.inkMuted,
            modifier = Modifier.padding(top = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun SourceHeader(state: SourceUiState, onClose: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.source_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        state.chunk?.let {
            Tag(
                text = stringResource(R.string.source_page, it.page),
                container = StudyTheme.colors.primaryTint,
                content = StudyTheme.colors.primaryStrong,
            )
        }
        if (onClose != null) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.source_close))
            }
        }
    }
}

@Composable
private fun SourceBody(state: SourceUiState, quote: String?, onRetry: () -> Unit) {
    when {
        state.isLoading -> Box(Modifier.height(160.dp)) { LoadingView() }
        state.error != null -> MessageView(
            title = stringResource(R.string.source_error_title),
            body = stringResource(state.error.messageRes()),
            actionLabel = stringResource(R.string.action_retry),
            onAction = onRetry,
        )
        state.chunk != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HighlightedSource(text = state.chunk.text, quote = quote)
            Text(
                stringResource(R.string.source_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = StudyTheme.colors.inkMuted,
            )
        }
    }
}

/** Đoạn nguồn với phần trích được tô vàng dần (FE.md 10.1: vệt vàng 700 ms, trễ 250 ms). */
@Composable
private fun HighlightedSource(text: String, quote: String?) {
    val colors = StudyTheme.colors
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(250)
        revealed = true
    }
    val highlight by animateColorAsState(
        targetValue = if (revealed) colors.highlight else Color.Transparent,
        animationSpec = tween(700),
        label = "highlight",
    )
    val plain = LatexToUnicode.convert(text)
    val start = quote?.let { plain.indexOf(it, ignoreCase = true) } ?: -1
    val annotated = buildAnnotatedString {
        if (quote != null && start >= 0) {
            append(plain.substring(0, start))
            withStyle(SpanStyle(background = highlight)) { append(plain.substring(start, start + quote.length)) }
            append(plain.substring(start + quote.length))
        } else {
            withStyle(SpanStyle(background = highlight.copy(alpha = highlight.alpha * 0.55f))) { append(plain) }
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge,
        color = colors.ink,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceMuted)
            .padding(16.dp),
    )
}

private fun sourceKey(request: SourceRequest) = "source/${request.documentId}/${request.chunkId}"
