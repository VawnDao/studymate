package com.studymate.app.ui.feature.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.studymate.app.ui.navigation.QuizRoute
import com.studymate.domain.model.QuizItem
import com.studymate.domain.model.QuizResult
import com.studymate.domain.usecase.ObserveStudyPackUseCase
import com.studymate.domain.usecase.SubmitQuizUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizUiState(
    val isLoading: Boolean = true,
    val items: List<QuizItem> = emptyList(),
    val index: Int = 0,
    val selected: Int? = null,
    val answers: Map<String, Int> = emptyMap(),
    val result: QuizResult? = null,
    val isSubmitting: Boolean = false,
    val chunkPages: Map<String, Int> = emptyMap(),
) {
    val current: QuizItem? get() = items.getOrNull(index)
    val isAnswered: Boolean get() = selected != null
    val isLast: Boolean get() = index == items.lastIndex
    val progress: Float
        get() = when {
            items.isEmpty() -> 0f
            result != null -> 1f
            else -> (index + if (isAnswered) 1 else 0).toFloat() / items.size
        }
}

sealed interface QuizEvent {
    data class Select(val option: Int) : QuizEvent
    data object Next : QuizEvent
    data object Retry : QuizEvent
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observePack: ObserveStudyPackUseCase,
    private val submitQuiz: SubmitQuizUseCase,
) : ViewModel() {

    val documentId: String = savedStateHandle.toRoute<QuizRoute>().documentId

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val pack = observePack(documentId).firstOrNull()
            _uiState.value = QuizUiState(
                isLoading = false,
                items = pack?.quiz.orEmpty(),
                chunkPages = pack?.chunkPages.orEmpty(),
            )
        }
    }

    fun onEvent(event: QuizEvent) {
        when (event) {
            is QuizEvent.Select -> select(event.option)
            QuizEvent.Next -> next()
            QuizEvent.Retry -> _uiState.update {
                QuizUiState(isLoading = false, items = it.items, chunkPages = it.chunkPages)
            }
        }
    }

    private fun select(option: Int) {
        _uiState.update { state ->
            val item = state.current
            if (item == null || state.isAnswered || option !in item.options.indices) {
                state
            } else {
                state.copy(selected = option, answers = state.answers + (item.id to option))
            }
        }
    }

    private fun next() {
        val state = _uiState.value
        if (!state.isAnswered || state.isSubmitting) return
        if (!state.isLast) {
            _uiState.update { it.copy(index = it.index + 1, selected = null) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = submitQuiz(documentId, state.items, state.answers)
            _uiState.update { it.copy(result = result, isSubmitting = false) }
        }
    }
}
