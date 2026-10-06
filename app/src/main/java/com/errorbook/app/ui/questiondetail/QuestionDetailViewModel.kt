package com.errorbook.app.ui.questiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.errorbook.app.data.local.entity.QuestionEntity
import com.errorbook.app.data.model.QuestionReasonDisplay
import com.errorbook.app.data.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuestionDetailUiState(
    val isLoading: Boolean = true,
    val question: QuestionEntity? = null,
    val reasons: List<QuestionReasonDisplay> = emptyList(),
    val deleted: Boolean = false,
)

@HiltViewModel
class QuestionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val questionRepository: QuestionRepository,
) : ViewModel() {

    private val questionId: Long = checkNotNull(savedStateHandle["questionId"])

    private val question = MutableStateFlow<QuestionEntity?>(null)
    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<QuestionDetailUiState> = combine(
        question,
        questionRepository.getReasonsForQuestion(questionId),
        deleted,
    ) { q, reasons, isDeleted ->
        QuestionDetailUiState(
            isLoading = false,
            question = q,
            reasons = reasons,
            deleted = isDeleted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuestionDetailUiState())

    init {
        viewModelScope.launch {
            question.value = questionRepository.getById(questionId)
        }
    }

    fun delete() {
        val target = question.value ?: return
        viewModelScope.launch {
            questionRepository.delete(target)
            deleted.value = true
        }
    }

    fun removeReason(questionReasonId: Long) {
        viewModelScope.launch {
            questionRepository.deleteReasonLink(questionReasonId)
        }
    }
}
