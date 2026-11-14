package com.studymate.app.ui.feature.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studymate.domain.common.AppResult
import com.studymate.domain.common.DomainError
import com.studymate.domain.model.DocumentInput
import com.studymate.domain.usecase.ImportDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class AddAction { PDF, CAMERA, GALLERY }

data class AddDocumentUiState(
    val isUploading: Boolean = false,
    val error: DomainError? = null,
)

sealed interface AddDocumentEffect {
    data class Uploaded(val documentId: String) : AddDocumentEffect
}

@HiltViewModel
class AddDocumentViewModel @Inject constructor(
    private val importDocument: ImportDocumentUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDocumentUiState())
    val uiState: StateFlow<AddDocumentUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AddDocumentEffect>(Channel.BUFFERED)
    val effects: Flow<AddDocumentEffect> = _effects.receiveAsFlow()

    fun import(input: DocumentInput) {
        if (_uiState.value.isUploading) return
        _uiState.value = AddDocumentUiState(isUploading = true)
        viewModelScope.launch {
            when (val result = importDocument(input)) {
                is AppResult.Success -> {
                    _uiState.value = AddDocumentUiState()
                    _effects.send(AddDocumentEffect.Uploaded(result.value.documentId))
                }
                is AppResult.Failure -> _uiState.value = AddDocumentUiState(error = result.error)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
