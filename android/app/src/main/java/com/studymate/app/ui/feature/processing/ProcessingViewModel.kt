package com.studymate.app.ui.feature.processing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.studymate.app.ui.navigation.ProcessingRoute
import com.studymate.domain.common.AppResult
import com.studymate.domain.common.DomainError
import com.studymate.domain.common.ErrorCode
import com.studymate.domain.model.ProcessingStatus
import com.studymate.domain.usecase.ObserveLibraryUseCase
import com.studymate.domain.usecase.ObserveProcessingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 4 bước hiển thị gộp từ trạng thái pipeline (FE.md mục 9). */
const val PROCESSING_STEPS = 4

fun ProcessingStatus.displayStep(): Int = when (this) {
    ProcessingStatus.UPLOADED, ProcessingStatus.EXTRACTING -> 0
    ProcessingStatus.OCR -> 1
    ProcessingStatus.CHUNKING, ProcessingStatus.INDEXING -> 2
    ProcessingStatus.GENERATING, ProcessingStatus.VALIDATING -> 3
    ProcessingStatus.READY -> PROCESSING_STEPS
    ProcessingStatus.FAILED -> -1
}

data class ProcessingUiState(
    val title: String? = null,
    /** Bước đang chạy (0..3); bằng [PROCESSING_STEPS] khi xong. */
    val currentStep: Int = 0,
    val failure: ErrorCode? = null,
    val error: DomainError? = null,
) {
    val progress: Float get() = currentStep.toFloat() / PROCESSING_STEPS
    val hasProblem: Boolean get() = failure != null || error != null
}

sealed interface ProcessingEffect {
    data class Completed(val documentId: String) : ProcessingEffect
}

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeProcessing: ObserveProcessingUseCase,
    observeLibrary: ObserveLibraryUseCase,
) : ViewModel() {

    val documentId: String = savedStateHandle.toRoute<ProcessingRoute>().documentId

    private val _uiState = MutableStateFlow(ProcessingUiState())
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ProcessingEffect>(Channel.BUFFERED)
    val effects: Flow<ProcessingEffect> = _effects.receiveAsFlow()

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            observeLibrary()
                .map { items -> items.firstOrNull { it.document.id == documentId }?.document?.title }
                .distinctUntilChanged()
                .collect { title -> _uiState.update { it.copy(title = title) } }
        }
        startPolling()
    }

    fun retry() = startPolling()

    private fun startPolling() {
        pollJob?.cancel()
        _uiState.update { it.copy(failure = null, error = null) }
        pollJob = viewModelScope.launch {
            observeProcessing(documentId).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        val state = result.value
                        if (state.status == ProcessingStatus.FAILED) {
                            _uiState.update { it.copy(failure = state.failure ?: ErrorCode.INTERNAL) }
                        } else {
                            _uiState.update { it.copy(currentStep = state.status.displayStep()) }
                            if (state.status == ProcessingStatus.READY) _effects.send(ProcessingEffect.Completed(documentId))
                        }
                    }
                    is AppResult.Failure -> _uiState.update { it.copy(error = result.error) }
                }
            }
        }
    }
}
