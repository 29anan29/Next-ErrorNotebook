package com.errorbook.app.ui.reasondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.errorbook.app.data.model.ReasonQuestionItem
import com.errorbook.app.data.model.ReasonWithCount
import com.errorbook.app.data.repository.QuestionRepository
import com.errorbook.app.data.repository.ReasonRepository
import com.errorbook.app.domain.usecase.RecordReviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReasonDetailUiState(
    val isLoading: Boolean = true,
    val reason: ReasonWithCount? = null,
    val questions: List<ReasonQuestionItem> = emptyList(),
    val currentIndex: Int = 0,
    /** 复习模式核心开关：默认 false —— 先看题目、想清楚为什么错，才揭示错因。 */
    val revealed: Boolean = false,
) {
    val currentItem: ReasonQuestionItem? get() = questions.getOrNull(currentIndex)
    val isLast: Boolean get() = currentIndex >= questions.lastIndex
}

@HiltViewModel
class ReasonDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    reasonRepository: ReasonRepository,
    questionRepository: QuestionRepository,
    private val recordReviewUseCase: RecordReviewUseCase,
) : ViewModel() {

    private val reasonId: Long = checkNotNull(savedStateHandle["reasonId"])

    private val currentIndex = MutableStateFlow(0)
    private val revealed = MutableStateFlow(false)

    val uiState: StateFlow<ReasonDetailUiState> = combine(
        reasonRepository.getDetail(reasonId),
        questionRepository.getQuestionsForReason(reasonId),
        currentIndex,
        revealed,
    ) { reason, questions, index, isRevealed ->
        ReasonDetailUiState(
            isLoading = false,
            reason = reason,
            questions = questions,
            currentIndex = index.coerceIn(0, (questions.size - 1).coerceAtLeast(0)),
            revealed = isRevealed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReasonDetailUiState())

    fun reveal() {
        revealed.value = true
    }

    /** 不记反馈、直接翻到下一题。 */
    fun next() {
        revealed.value = false
        currentIndex.value += 1
    }

    /** 记录复习反馈；「又错」会让该错因的出错次数 +1（见 [RecordReviewUseCase]）。 */
    fun onReviewed(result: String) {
        val item = uiState.value.currentItem ?: return
        viewModelScope.launch {
            recordReviewUseCase(
                questionReasonId = item.questionReasonId,
                questionId = item.questionId,
                reasonId = reasonId,
                result = result,
            )
            next()
        }
    }
}