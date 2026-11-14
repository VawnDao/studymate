package com.studymate.app.ui.feature.source

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studymate.domain.common.AppResult
import com.studymate.domain.common.DomainError
import com.studymate.domain.model.SourceChunk
import com.studymate.domain.usecase.GetSourceChunkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SourceUiState(
    val isLoading: Boolean = true,
    val chunk: SourceChunk? = null,
    val error: DomainError? = null,
)

@HiltViewModel
class SourceViewModel @Inject constructor(
    private val getSourceChunk: GetSourceChunkUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SourceUiState())
    val uiState: StateFlow<SourceUiState> = _uiState.asStateFlow()

    private var loadedKey: String? = null
    private var job: Job? = null

    fun load(documentId: String, chunkId: String, force: Boolean = false) {
        val key = "$documentId/$chunkId"
        if (!force && key == loadedKey) return
        loadedKey = key
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            _uiState.value = when (val result = getSourceChunk(documentId, chunkId)) {
                is AppResult.Success -> SourceUiState(isLoading = false, chunk = result.value)
                is AppResult.Failure -> SourceUiState(isLoading = false, error = result.error)
            }
        }
    }
}
