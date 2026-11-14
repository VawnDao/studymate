package com.studymate.app.ui.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studymate.domain.model.TodayOverview
import com.studymate.domain.usecase.ObserveTodayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface TodayUiState {
    data object Loading : TodayUiState
    data class Ready(val overview: TodayOverview) : TodayUiState
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    observeToday: ObserveTodayUseCase,
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = observeToday()
        .map<TodayOverview, TodayUiState> { TodayUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)
}
