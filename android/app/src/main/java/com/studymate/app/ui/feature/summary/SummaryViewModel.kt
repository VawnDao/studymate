package com.studymate.app.ui.feature.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.studymate.app.ui.navigation.SummaryRoute
import com.studymate.domain.model.StudyPack
import com.studymate.domain.usecase.ObserveStudyPackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class SummaryUiState(
    val isLoading: Boolean = true,
    val pack: StudyPack? = null,
    val expanded: Set<Int> = setOf(0),
)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePack: ObserveStudyPackUseCase,
) : ViewModel() {

    val documentId: String = savedStateHandle.toRoute<SummaryRoute>().documentId

    private val expanded = MutableStateFlow(setOf(0))

    val uiState: StateFlow<SummaryUiState> = combine(observePack(documentId), expanded) { pack, open ->
        SummaryUiState(isLoading = false, pack = pack, expanded = open)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState())

    fun toggleSection(index: Int) {
        expanded.update { if (index in it) it - index else it + index }
    }
}
