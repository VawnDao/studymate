package com.studymate.app.ui.feature.cards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.studymate.app.ui.navigation.CardsRoute
import com.studymate.domain.model.Flashcard
import com.studymate.domain.model.ReviewRating
import com.studymate.domain.usecase.GetReviewSessionUseCase
import com.studymate.domain.usecase.ObserveStudyPackUseCase
import com.studymate.domain.usecase.ReviewCardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CardsUiState(
    val isLoading: Boolean = true,
    val cards: List<Flashcard> = emptyList(),
    val index: Int = 0,
    val isFlipped: Boolean = false,
    val isPractice: Boolean = false,
    val isRating: Boolean = false,
    val previews: Map<ReviewRating, Duration> = emptyMap(),
    val ratingCounts: Map<ReviewRating, Int> = emptyMap(),
    val isFinished: Boolean = false,
    /** Trang của đoạn nguồn (chỉ có khi ôn một tài liệu). */
    val chunkPages: Map<String, Int> = emptyMap(),
) {
    val current: Flashcard? get() = cards.getOrNull(index)
    val progress: Float get() = if (cards.isEmpty()) 0f else (if (isFinished) cards.size else index).toFloat() / cards.size
    val remembered: Int get() = ratingCounts.filterKeys { it != ReviewRating.AGAIN }.values.sum()
}

sealed interface CardsEvent {
    data object Flip : CardsEvent
    data class Rate(val rating: ReviewRating) : CardsEvent
    data object Restart : CardsEvent
}

@HiltViewModel
class CardsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getReviewSession: GetReviewSessionUseCase,
    private val reviewCard: ReviewCardUseCase,
    private val observePack: ObserveStudyPackUseCase,
) : ViewModel() {

    val documentId: String? = savedStateHandle.toRoute<CardsRoute>().documentId

    private val _uiState = MutableStateFlow(CardsUiState())
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: CardsEvent) {
        when (event) {
            CardsEvent.Flip -> _uiState.update { if (it.isFinished || it.current == null) it else it.copy(isFlipped = !it.isFlipped) }
            is CardsEvent.Rate -> rate(event.rating)
            CardsEvent.Restart -> load()
        }
    }

    private fun load() {
        _uiState.value = CardsUiState(isLoading = true)
        viewModelScope.launch {
            val session = getReviewSession(documentId)
            val pages = documentId?.let { observePack(it).firstOrNull()?.chunkPages }.orEmpty()
            val first = session.cards.firstOrNull()
            _uiState.value = CardsUiState(
                isLoading = false,
                cards = session.cards,
                isPractice = session.isPractice,
                previews = first?.let(reviewCard::previews).orEmpty(),
                chunkPages = pages,
            )
        }
    }

    private fun rate(rating: ReviewRating) {
        val state = _uiState.value
        val card = state.current ?: return
        if (!state.isFlipped || state.isRating) return
        _uiState.update { it.copy(isRating = true) }
        viewModelScope.launch {
            reviewCard(card, rating)
            _uiState.update { current ->
                val counts = current.ratingCounts + (rating to (current.ratingCounts[rating] ?: 0) + 1)
                val nextIndex = current.index + 1
                val next = current.cards.getOrNull(nextIndex)
                current.copy(
                    index = if (next != null) nextIndex else current.index,
                    isFlipped = false,
                    isRating = false,
                    ratingCounts = counts,
                    isFinished = next == null,
                    previews = next?.let(reviewCard::previews).orEmpty(),
                )
            }
        }
    }
}
