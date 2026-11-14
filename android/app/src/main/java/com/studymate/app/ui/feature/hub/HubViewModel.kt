package com.studymate.app.ui.feature.hub

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.studymate.app.ui.navigation.HubRoute
import com.studymate.domain.common.AppResult
import com.studymate.domain.common.DomainError
import com.studymate.domain.model.DocumentHub
import com.studymate.domain.usecase.ObserveDocumentHubUseCase
import com.studymate.domain.usecase.ObserveStudyPackUseCase
import com.studymate.domain.usecase.SyncStudyPackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HubUiState(
    val isLoading: Boolean = true,
    val hub: DocumentHub? = null,
    val error: DomainError? = null,
)

@HiltViewModel
class HubViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeHub: ObserveDocumentHubUseCase,
    private val observePack: ObserveStudyPackUseCase,
    private val syncPack: SyncStudyPackUseCase,
) : ViewModel() {

    val documentId: String = savedStateHandle.toRoute<HubRoute>().documentId

    private val syncError = MutableStateFlow<DomainError?>(null)
    private val syncing = MutableStateFlow(false)

    val uiState: StateFlow<HubUiState> = combine(observeHub(documentId), syncError, syncing) { hub, error, isSyncing ->
        when {
            hub != null -> HubUiState(isLoading = false, hub = hub)
            error != null && !isSyncing -> HubUiState(isLoading = false, error = error)
            else -> HubUiState(isLoading = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HubUiState())

    init {
        // Mở từ Thư viện khi bộ ôn tập chưa có trên máy (cài lại app, xoá dữ liệu…): tải về.
        viewModelScope.launch {
            if (observePack(documentId).first() == null) sync()
        }
    }

    fun retry() {
        viewModelScope.launch { sync() }
    }

    private suspend fun sync() {
        if (syncing.value) return
        syncing.value = true
        syncError.value = (syncPack(documentId) as? AppResult.Failure)?.error
        syncing.value = false
    }
}
