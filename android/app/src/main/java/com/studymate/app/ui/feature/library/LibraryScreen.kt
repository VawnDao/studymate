package com.studymate.app.ui.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
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
                itemsIndexed(state.items, key = { _, item -> item.document.id }) { index, item ->
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
