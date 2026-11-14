package com.studymate.app.ui.feature.tutor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.studymate.app.ui.navigation.TutorRoute
import com.studymate.domain.common.AppResult
import com.studymate.domain.common.DomainError
import com.studymate.domain.model.Citation
import com.studymate.domain.model.HintLevel
import com.studymate.domain.model.TutorMessage
import com.studymate.domain.model.TutorMode
import com.studymate.domain.model.TutorRole
import com.studymate.domain.usecase.AskTutorUseCase
import com.studymate.domain.usecase.ObserveStudyPackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatItem(
    val id: Long,
    val role: TutorRole,
    val text: String,
    val mode: TutorMode? = null,
    val citations: List<Citation> = emptyList(),
    val nextQuestion: String? = null,
)

data class TutorUiState(
    val title: String = "",
    val items: List<ChatItem> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val hintLevel: Int = HintLevel.MIN,
    val error: DomainError? = null,
) {
    private val lastReply: ChatItem? get() = items.lastOrNull()?.takeIf { it.role == TutorRole.ASSISTANT && it.mode != null }

    /** Chỉ cho xin thêm gợi ý / lời giải khi gia sư vừa gợi ý hoặc nhận xét. */
    val canAskForMore: Boolean
        get() = !isSending && error == null && lastReply?.mode.let { it == TutorMode.HINT || it == TutorMode.CHECK }

    val canSend: Boolean get() = !isSending && input.isNotBlank()
}

sealed interface TutorEvent {
    data class InputChanged(val text: String) : TutorEvent
    data object Send : TutorEvent

    /** [prompt] là câu người dùng gửi kèm (lấy từ string resource ở UI). */
    data class MoreHint(val prompt: String) : TutorEvent
    data class ShowSolution(val prompt: String) : TutorEvent
    data object Retry : TutorEvent
}

@HiltViewModel
class TutorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val askTutor: AskTutorUseCase,
    observePack: ObserveStudyPackUseCase,
) : ViewModel() {

    val documentId: String = savedStateHandle.toRoute<TutorRoute>().documentId

    private val _uiState = MutableStateFlow(TutorUiState())
    val uiState: StateFlow<TutorUiState> = _uiState.asStateFlow()

    private var nextId = 0L
    private var pendingHintLevel = HintLevel.MIN

    init {
        viewModelScope.launch {
            val title = observePack(documentId).firstOrNull()?.title.orEmpty()
            _uiState.update { it.copy(title = title) }
        }
    }

    fun onEvent(event: TutorEvent) {
        when (event) {
            is TutorEvent.InputChanged -> _uiState.update { it.copy(input = event.text.take(AskTutorUseCase.MAX_CHARS)) }
            TutorEvent.Send -> {
                val text = _uiState.value.input.trim()
                if (text.isEmpty()) return
                // Câu hỏi mới sau lời giải / câu ngoài tài liệu: quay về gợi ý mức 1.
                val lastMode = _uiState.value.items.lastOrNull { it.role == TutorRole.ASSISTANT }?.mode
                val level = if (lastMode == TutorMode.SOLUTION || lastMode == TutorMode.OUT_OF_SCOPE) {
                    HintLevel.MIN
                } else {
                    _uiState.value.hintLevel
                }
                send(text, level, clearInput = true)
            }
            is TutorEvent.MoreHint -> send(event.prompt, (_uiState.value.hintLevel + 1).coerceAtMost(HintLevel.MAX - 1))
            is TutorEvent.ShowSolution -> send(event.prompt, HintLevel.SOLUTION)
            TutorEvent.Retry -> request(pendingHintLevel)
        }
    }

    private fun send(text: String, hintLevel: Int, clearInput: Boolean = false) {
        if (_uiState.value.isSending) return
        _uiState.update {
            it.copy(
                items = it.items + ChatItem(id = nextId++, role = TutorRole.USER, text = text),
                input = if (clearInput) "" else it.input,
                error = null,
            )
        }
        request(hintLevel)
    }

    private fun request(hintLevel: Int) {
        pendingHintLevel = hintLevel
        _uiState.update { it.copy(isSending = true, error = null) }
        viewModelScope.launch {
            val history = _uiState.value.items.map { TutorMessage(it.role, it.text) }
            when (val result = askTutor(documentId, history, hintLevel)) {
                is AppResult.Success -> {
                    val reply = result.value
                    _uiState.update {
                        it.copy(
                            items = it.items + ChatItem(
                                id = nextId++,
                                role = TutorRole.ASSISTANT,
                                text = reply.reply,
                                mode = reply.mode,
                                citations = reply.citations,
                                nextQuestion = reply.nextQuestion,
                            ),
                            isSending = false,
                            hintLevel = hintLevel,
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update { it.copy(isSending = false, error = result.error) }
            }
        }
    }
}
