package com.studymate.app.ui.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import com.studymate.app.ui.adaptive.AdaptiveWidth
import com.studymate.app.ui.adaptive.ContentWidth
import com.studymate.app.ui.adaptive.centeredContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studymate.app.R
import com.studymate.app.ui.common.ObserveAsEvents
import com.studymate.app.ui.common.messageRes
import com.studymate.app.ui.components.DocumentRow
import com.studymate.app.ui.components.LoadingView
import com.studymate.app.ui.components.MessageView
import com.studymate.app.ui.components.staggerIn
import com.studymate.app.ui.theme.StudyTheme
import com.studymate.domain.model.DocumentWithDue
import kotlinx.coroutines.launch

@Composable
fun LibraryRoute(
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onOpenDocument: (DocumentWithDue) -> Unit,
    onAdd: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is LibraryEffect.ShowError -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(effect.error.messageRes()))
            }
        }
    }
    LibraryScreen(state, contentPadding, onOpenDocument, onAdd, viewModel::onEvent)
}

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    contentPadding: PaddingValues,
    onOpenDocument: (DocumentWithDue) -> Unit,
    onAdd: () -> Unit,
    onEvent: (LibraryEvent) -> Unit,
) {
    when {
        state.isLoading -> LoadingView()
        !AdaptiveWidth.current.isCompact -> LibraryGrid(state, contentPadding, onOpenDocument, onAdd, onEvent)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "title") {
                Text(stringResource(R.string.library_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.staggerIn(0))
            }
            if (state.items.isEmpty()) {
                item(key = "empty") {
                    MessageView(
                        title = stringResource(R.string.library_empty_title),
                        body = stringResource(R.string.library_empty_body),
                        actionLabel = stringResource(R.string.add_document),
                        onAction = onAdd,
                    )
                }
            } else {
                item(key = "hint") {
                    Text(
                        stringResource(R.string.library_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudyTheme.colors.inkMuted,
                    )
                }
                if (state.subjects.isNotEmpty()) {
                    item(key = "subjects") {
                        SubjectFilterRow(
                            subjects = state.subjects,
                            selected = state.selectedSubject,
                            totalCount = state.items.size,
                            onSelect = { onEvent(LibraryEvent.SelectSubject(it)) },
                        )
                    }
                }
                itemsIndexed(state.filteredItems, key = { _, item -> item.document.id }) { index, item ->
                    DocumentRow(
                        item = item,
                        onClick = { onOpenDocument(item) },
                        onLongClick = { onEvent(LibraryEvent.RequestDelete(item.document)) },
                        modifier = Modifier
                            .animateItem()
                            .staggerIn(index + 1),
                    )
                }
            }
        }
    }

    state.pendingDelete?.let { document ->
        AlertDialog(
            onDismissRequest = { onEvent(LibraryEvent.DismissDelete) },
            title = { Text(stringResource(R.string.library_delete_title)) },
            text = { Text(stringResource(R.string.library_delete_body, document.title)) },
            confirmButton = {
                TextButton(onClick = { onEvent(LibraryEvent.ConfirmDelete) }, enabled = !state.isDeleting) {
                    Text(stringResource(R.string.library_delete), color = StudyTheme.colors.errorText)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(LibraryEvent.DismissDelete) }, enabled = !state.isDeleting) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** Tablet: lưới thẻ tài liệu, số cột tự co giãn theo bề rộng (mỗi cột tối thiểu 320dp). */
@Composable
private fun LibraryGrid(
    state: LibraryUiState,
    contentPadding: PaddingValues,
    onOpenDocument: (DocumentWithDue) -> Unit,
    onAdd: () -> Unit,
    onEvent: (LibraryEvent) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = Modifier
            .fillMaxSize()
            .centeredContent(ContentWidth.Wide),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.staggerIn(0)) {
                Text(stringResource(R.string.library_title), style = MaterialTheme.typography.headlineLarge)
                if (state.items.isNotEmpty()) {
                    Text(stringResource(R.string.library_hint), style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.inkMuted)
                }
            }
        }
        if (state.items.isEmpty()) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                MessageView(
                    title = stringResource(R.string.library_empty_title),
                    body = stringResource(R.string.library_empty_body),
                    actionLabel = stringResource(R.string.add_document),
                    onAction = onAdd,
                    modifier = Modifier.centeredContent(ContentWidth.Focus),
                )
            }
        } else {
            if (state.subjects.isNotEmpty()) {
                item(key = "subjects", span = { GridItemSpan(maxLineSpan) }) {
                    SubjectFilterRow(
                        subjects = state.subjects,
                        selected = state.selectedSubject,
                        totalCount = state.items.size,
                        onSelect = { onEvent(LibraryEvent.SelectSubject(it)) },
                    )
                }
            }
            gridItemsIndexed(state.filteredItems, key = { _, item -> item.document.id }) { index, item ->
                DocumentRow(
                    item = item,
                    onClick = { onOpenDocument(item) },
                    onLongClick = { onEvent(LibraryEvent.RequestDelete(item.document)) },
                    modifier = Modifier
                        .animateItem()
                        .staggerIn(index + 1),
                )
            }
        }
    }
}

@Composable
private fun SubjectFilterRow(
    subjects: List<SubjectCount>,
    selected: String?,
    totalCount: Int,
    onSelect: (String?) -> Unit,
) {
    val colors = StudyTheme.colors
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = colors.primaryTint,
        selectedLabelColor = colors.primaryStrong,
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.library_subject_chip, stringResource(R.string.library_subject_all), totalCount)) },
                colors = chipColors,
            )
        }
        items(subjects, key = { it.name }) { subject ->
            FilterChip(
                selected = selected == subject.name,
                onClick = { onSelect(if (selected == subject.name) null else subject.name) },
                label = { Text(stringResource(R.string.library_subject_chip, subject.name, subject.count)) },
                colors = chipColors,
            )
        }
    }
}
