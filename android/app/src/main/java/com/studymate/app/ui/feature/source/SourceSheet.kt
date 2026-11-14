package com.studymate.app.ui.feature.source

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSheet(
    request: SourceRequest,
    onDismiss: () -> Unit,
    viewModel: SourceViewModel = hiltViewModel(key = "source/${request.documentId}/${request.chunkId}"),
) {
    LaunchedEffect(request) { viewModel.load(request.documentId, request.chunkId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.source_title), style = MaterialTheme.typography.titleLarge)
                state.chunk?.let {
                    Tag(
                        text = stringResource(R.string.source_page, it.page),
                        container = StudyTheme.colors.primaryTint,
                        content = StudyTheme.colors.primaryStrong,
                    )
                }
            }
            when {
                state.isLoading -> Box(Modifier.height(160.dp)) { LoadingView() }
                state.error != null -> MessageView(
                    title = stringResource(R.string.source_error_title),
                    body = stringResource(state.error!!.messageRes()),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = { viewModel.load(request.documentId, request.chunkId, force = true) },
                )
                state.chunk != null -> {
                    HighlightedSource(text = state.chunk!!.text, quote = request.quote)
                    Text(
                        stringResource(R.string.source_footnote),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudyTheme.colors.inkMuted,
                    )
                }
            }
            StudyButton(text = stringResource(R.string.action_understood), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
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
        if (start >= 0) {
            append(plain.substring(0, start))
            withStyle(SpanStyle(background = highlight)) { append(plain.substring(start, start + quote!!.length)) }
            append(plain.substring(start + quote!!.length))
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
