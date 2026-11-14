package com.studymate.app.ui.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studymate.domain.common.AppResult
import com.studymate.domain.common.DomainError
import com.studymate.domain.model.Document
import com.studymate.domain.model.DocumentWithDue
import com.studymate.domain.usecase.DeleteDocumentUseCase
import com.studymate.domain.usecase.ObserveLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubjectCount(
    val name: String,
    val count: Int,
)

data class LibraryUiState(
    val isLoading: Boolean = true,
    val items: List<DocumentWithDue> = emptyList(),
    val filteredItems: List<DocumentWithDue> = emptyList(),
    val subjects: List<SubjectCount> = emptyList(),
    val selectedSubject: String? = null,
    val pendingDelete: Document? = null,
    val isDeleting: Boolean = false,
)

sealed interface LibraryEvent {
    data class SelectSubject(val subject: String?) : LibraryEvent
    data class RequestDelete(val document: Document) : LibraryEvent
    data object ConfirmDelete : LibraryEvent
    data object DismissDelete : LibraryEvent
}

sealed interface LibraryEffect {
    data class ShowError(val error: DomainError) : LibraryEffect
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    observeLibrary: ObserveLibraryUseCase,
    private val deleteDocument: DeleteDocumentUseCase,
) : ViewModel() {

    private data class DeleteState(val pending: Document? = null, val isDeleting: Boolean = false)

    private val deleteState = MutableStateFlow(DeleteState())
    private val selectedSubject = MutableStateFlow<String?>(null)
    private val _effects = Channel<LibraryEffect>(Channel.BUFFERED)
    val effects: Flow<LibraryEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<LibraryUiState> = combine(observeLibrary(), selectedSubject, deleteState) { items, selected, delete ->
        val subjects = items
            .mapNotNull { it.document.subject.trim().takeIf(String::isNotEmpty) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { SubjectCount(it.key, it.value) }
        val activeSubject = selected?.takeIf { s -> subjects.any { it.name == s } }
        val filtered = if (activeSubject == null) items else items.filter { it.document.subject.trim() == activeSubject }
        LibraryUiState(
            isLoading = false,
            items = items,
            filteredItems = filtered,
            subjects = subjects,
            selectedSubject = activeSubject,
            pendingDelete = delete.pending,
            isDeleting = delete.isDeleting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.SelectSubject -> selectedSubject.value = event.subject
            is LibraryEvent.RequestDelete -> deleteState.value = DeleteState(pending = event.document)
            LibraryEvent.DismissDelete -> if (!deleteState.value.isDeleting) deleteState.value = DeleteState()
            LibraryEvent.ConfirmDelete -> confirmDelete()
        }
    }

    private fun confirmDelete() {
        val document = deleteState.value.pending ?: return
        if (deleteState.value.isDeleting) return
        deleteState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            val result = deleteDocument(document.id)
            deleteState.value = DeleteState()
            if (result is AppResult.Failure) _effects.send(LibraryEffect.ShowError(result.error))
        }
    }
}
