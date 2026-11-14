package com.studymate.app.ui.feature.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.studymate.app.ui.navigation.PodcastRoute
import com.studymate.domain.common.AppResult
import com.studymate.domain.common.DomainError
import com.studymate.domain.common.ErrorCode
import com.studymate.domain.model.Podcast
import com.studymate.domain.model.PodcastStatus
import com.studymate.domain.usecase.GetPodcastAccessTokenUseCase
import com.studymate.domain.usecase.GetPodcastStatusUseCase
import com.studymate.domain.usecase.ObservePodcastUseCase
import com.studymate.domain.usecase.StartPodcastUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PodcastUiState(
    val podcast: Podcast? = null,
    val isStarting: Boolean = false,
    val isLoading: Boolean = false,
    val error: DomainError? = null,
    val accessToken: String? = null,
)

@HiltViewModel
class PodcastViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val startPodcast: StartPodcastUseCase,
    private val getPodcastStatus: GetPodcastStatusUseCase,
    private val observePodcast: ObservePodcastUseCase,
    private val accessToken: GetPodcastAccessTokenUseCase,
) : ViewModel() {
    val documentId: String = savedStateHandle.toRoute<PodcastRoute>().documentId
    private val _uiState = MutableStateFlow(PodcastUiState())
    val uiState: StateFlow<PodcastUiState> = _uiState.asStateFlow()
    private var pollJob: Job? = null

    init {
        loadExisting()
    }

    private fun loadExisting() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = getPodcastStatus(documentId)) {
                is AppResult.Success -> {
                    val podcast = result.value
                    _uiState.value = _uiState.value.copy(
                        podcast = podcast,
                        isLoading = podcast.status == PodcastStatus.GENERATING,
                        accessToken = if (podcast.status == PodcastStatus.READY) accessToken() else null,
                        error = null,
                    )
                    if (podcast.status == PodcastStatus.GENERATING) observe()
                }
                is AppResult.Failure -> {
                    val notCreated = (result.error as? DomainError.Api)?.code == ErrorCode.NOT_FOUND
                    _uiState.value = if (notCreated) {
                        PodcastUiState()
                    } else {
                        _uiState.value.copy(isLoading = false, error = result.error)
                    }
                }
            }
        }
    }

    fun create() {
        if (_uiState.value.isStarting || _uiState.value.isLoading) return
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStarting = true, error = null)
            when (val result = startPodcast(documentId)) {
                is AppResult.Failure -> _uiState.value = PodcastUiState(error = result.error)
                is AppResult.Success -> {
                    _uiState.value = PodcastUiState(podcast = result.value, isLoading = result.value.status == PodcastStatus.GENERATING)
                    observe()
                }
            }
        }
    }

    fun retry() = create()

    private suspend fun observe() {
        observePodcast(documentId).collect { result ->
            when (result) {
                is AppResult.Failure -> _uiState.value = _uiState.value.copy(isStarting = false, isLoading = false, error = result.error)
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        podcast = result.value,
                        isStarting = false,
                        isLoading = result.value.status == PodcastStatus.GENERATING,
                        accessToken = if (result.value.status == PodcastStatus.READY) accessToken() else _uiState.value.accessToken,
                        error = null,
                    )
                }
            }
        }
    }
}
