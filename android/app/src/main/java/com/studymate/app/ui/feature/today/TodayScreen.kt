package com.studymate.app.ui.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.studymate.app.ui.adaptive.AdaptiveWidth
import com.studymate.app.ui.adaptive.ContentWidth
import com.studymate.app.ui.adaptive.centeredContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studymate.app.R
import com.studymate.app.ui.components.DocumentRow
import com.studymate.app.ui.components.LoadingView
import com.studymate.app.ui.components.PressableCard
import com.studymate.app.ui.components.SectionTitle
import com.studymate.app.ui.components.StudyButton
import com.studymate.app.ui.components.StudyButtonStyle
import com.studymate.app.ui.components.staggerIn
import com.studymate.app.ui.feature.add.AddAction
import com.studymate.app.ui.theme.StudyTheme
import com.studymate.domain.model.DocumentWithDue
import com.studymate.domain.model.TodayOverview

@Composable
fun TodayRoute(
    contentPadding: PaddingValues,
    onReviewDue: () -> Unit,
    onOpenDocument: (DocumentWithDue) -> Unit,
    onAdd: (AddAction?) -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(state, contentPadding, onReviewDue, onOpenDocument, onAdd)
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    contentPadding: PaddingValues,
    onReviewDue: () -> Unit,
    onOpenDocument: (DocumentWithDue) -> Unit,
    onAdd: (AddAction?) -> Unit,
) {
    when (state) {
        TodayUiState.Loading -> LoadingView()
        is TodayUiState.Ready -> TodayContent(state.overview, contentPadding, onReviewDue, onOpenDocument, onAdd)
    }
}

@Composable
private fun TodayContent(
    overview: TodayOverview,
    contentPadding: PaddingValues,
    onReviewDue: () -> Unit,
    onOpenDocument: (DocumentWithDue) -> Unit,
    onAdd: (AddAction?) -> Unit,
) {
    if (AdaptiveWidth.current.isExpanded) {
        TodayTwoPane(overview, contentPadding, onReviewDue, onOpenDocument, onAdd)
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .centeredContent(ContentWidth.Reading),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "header") {
            Column(Modifier.staggerIn(0)) {
                Text(stringResource(R.string.today_greeting), style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.inkMuted)
                Text(stringResource(R.string.today_title), style = MaterialTheme.typography.headlineMedium)
            }
        }
        item(key = "hero") {
            HeroCard(overview, onReviewDue, onAdd, Modifier.staggerIn(1))
        }
        item(key = "shortcuts") {
            Row(Modifier.staggerIn(2), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShortcutTile(
                    title = stringResource(R.string.add_camera),
                    subtitle = stringResource(R.string.add_camera_sub_short),
                    icon = Icons.Outlined.CameraAlt,
                    container = StudyTheme.colors.peach,
                    onClick = { onAdd(AddAction.CAMERA) },
                    modifier = Modifier.weight(1f),
                )
                ShortcutTile(
                    title = stringResource(R.string.add_pdf),
                    subtitle = stringResource(R.string.add_pdf_sub_short),
                    icon = Icons.Outlined.PictureAsPdf,
                    container = StudyTheme.colors.sky,
                    onClick = { onAdd(AddAction.PDF) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (overview.recent.isNotEmpty()) {
            item(key = "recent-title") { SectionTitle(stringResource(R.string.today_recent), Modifier.staggerIn(3)) }
            items(overview.recent, key = { it.document.id }) { item ->
                DocumentRow(item = item, onClick = { onOpenDocument(item) }, modifier = Modifier.animateItem())
            }
        }
    }
}

/** Tablet ngang: trái là thẻ chính + lối tắt, phải là danh sách tài liệu gần đây (cuộn riêng). */
@Composable
private fun TodayTwoPane(
    overview: TodayOverview,
    contentPadding: PaddingValues,
    onReviewDue: () -> Unit,
    onOpenDocument: (DocumentWithDue) -> Unit,
    onAdd: (AddAction?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .centeredContent(ContentWidth.Wide)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1.1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(Modifier.staggerIn(0)) {
                Text(stringResource(R.string.today_greeting), style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.inkMuted)
                Text(stringResource(R.string.today_title), style = MaterialTheme.typography.headlineLarge)
            }
            HeroCard(overview, onReviewDue, onAdd, Modifier.staggerIn(1))
            Row(Modifier.staggerIn(2), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShortcutTile(
                    title = stringResource(R.string.add_camera),
                    subtitle = stringResource(R.string.add_camera_sub_short),
                    icon = Icons.Outlined.CameraAlt,
                    container = StudyTheme.colors.peach,
                    onClick = { onAdd(AddAction.CAMERA) },
                    modifier = Modifier.weight(1f),
                )
                ShortcutTile(
                    title = stringResource(R.string.add_pdf),
                    subtitle = stringResource(R.string.add_pdf_sub_short),
                    icon = Icons.Outlined.PictureAsPdf,
                    container = StudyTheme.colors.sky,
                    onClick = { onAdd(AddAction.PDF) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "recent-title") {
                SectionTitle(stringResource(R.string.today_recent), Modifier.padding(top = 8.dp).staggerIn(3))
            }
            if (overview.recent.isEmpty()) {
                item(key = "recent-empty") {
                    Text(
                        stringResource(R.string.library_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudyTheme.colors.inkMuted,
                    )
                }
            }
            items(overview.recent, key = { it.document.id }) { item ->
                DocumentRow(item = item, onClick = { onOpenDocument(item) }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun HeroCard(overview: TodayOverview, onReviewDue: () -> Unit, onAdd: (AddAction?) -> Unit, modifier: Modifier) {
    val colors = StudyTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.primary,
        contentColor = Color.White,
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                overview.dueTotal > 0 -> {
                    Text(stringResource(R.string.today_due_label), style = MaterialTheme.typography.labelMedium)
                    Text(
                        pluralStringResource(R.plurals.due_cards_big, overview.dueTotal, overview.dueTotal),
                        style = MaterialTheme.typography.displayLarge,
                    )
                    overview.focus?.let {
                        Text(it.document.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
                    StudyButton(
                        text = stringResource(R.string.today_review_now),
                        onClick = onReviewDue,
                        style = StudyButtonStyle.Light,
                        height = 52.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                overview.documentCount > 0 -> {
                    Text(stringResource(R.string.today_done_label), style = MaterialTheme.typography.labelMedium)
                    Text(stringResource(R.string.today_done_title), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.today_done_body), style = MaterialTheme.typography.bodyMedium)
                    StudyButton(
                        text = stringResource(R.string.add_document),
                        onClick = { onAdd(null) },
                        style = StudyButtonStyle.Light,
                        height = 52.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    Text(stringResource(R.string.today_start_label), style = MaterialTheme.typography.labelMedium)
                    Text(stringResource(R.string.today_start_title), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.today_start_body), style = MaterialTheme.typography.bodyMedium)
                    StudyButton(
                        text = stringResource(R.string.add_document),
                        onClick = { onAdd(null) },
                        style = StudyButtonStyle.Light,
                        height = 52.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    PressableCard(onClick = onClick, color = container, shape = RoundedCornerShape(22.dp), modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.inkSecondary)
    }
}
