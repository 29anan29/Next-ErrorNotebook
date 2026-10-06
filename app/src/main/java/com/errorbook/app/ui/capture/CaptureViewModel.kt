package com.errorbook.app.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.errorbook.app.data.local.entity.QuestionEntity
import com.errorbook.app.domain.usecase.RecognizeTextUseCase
import com.errorbook.app.domain.usecase.SaveQuestionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CaptureUiState {
    data class Ready(val subjectId: Long?) : CaptureUiState
    data class Processing(val imageUri: android.net.Uri, val subjectId: Long?) : CaptureUiState
    data class OcrResult(
        val imageUri: android.net.Uri,
        val ocrText: String?,
        val subjectId: Long?,
    ) : CaptureUiState
    data class Error(val message: String) : CaptureUiState
}

data class CaptureFormState(
    val ocrText: String = "",
    val note: String = "",
    val source: String = "",
    val subjectId: Long? = null,
    val reasonLinks: List<ReasonLink> = emptyList(),
) {
    data class ReasonLink(val reasonId: Long, val detail: String?)
}

class CaptureViewModel @Inject constructor(
    private val recognizeTextUseCase: RecognizeTextUseCase,
    private val saveQuestionUseCase: SaveQuestionUseCase,
    private val reasonRepository: com.errorbook.app.data.repository.ReasonRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Ready(null))
    val uiState: Flow<CaptureUiState> = _uiState

    private val _formState = MutableStateFlow(CaptureFormState())
    val formState: Flow<CaptureFormState> = _formState

    val recentReasons: Flow<List<com.errorbook.app.data.local.entity.ReasonEntity>> =
        reasonRepository.getRecentUsed(10)

    fun onImageSelected(imageUri: android.net.Uri, subjectId: Long?) {
        _uiState.value = CaptureUiState.Processing(imageUri, subjectId)
        viewModelScope.launch {
            val result = recognizeTextUseCase(imageUri)
            result.fold(
                onSuccess = { text ->
                    _formState.value = _formState.value.copy(ocrText = text)
                    _uiState.value = CaptureUiState.OcrResult(imageUri, text, subjectId)
                },
                onFailure = { _uiState.value = CaptureUiState.Error(it.message ?: "识别失败") },
            )
        }
    }

    fun updateOcrText(text: String) {
        _formState.value = _formState.value.copy(ocrText = text)
    }

    fun updateNote(note: String) {
        _formState.value = _formState.value.copy(note = note)
    }

    fun updateSource(source: String) {
        _formState.value = _formState.value.copy(source = source)
    }

    fun selectSubject(subjectId: Long?) {
        _formState.value = _formState.value.copy(subjectId = subjectId)
        when (val current = _uiState.value) {
            is CaptureUiState.Ready -> _uiState.value = CaptureUiState.Ready(subjectId)
            is CaptureUiState.Processing -> _uiState.value = CaptureUiState.Processing(current.imageUri, subjectId)
            is CaptureUiState.OcrResult -> _uiState.value = CaptureUiState.OcrResult(current.imageUri, current.ocrText, subjectId)
            is CaptureUiState.Error -> {}
        }
    }

    fun addReason(reasonId: Long, detail: String?) {
        val links = _formState.value.reasonLinks.toMutableList()
        if (links.none { it.reasonId == reasonId }) {
            links.add(CaptureFormState.ReasonLink(reasonId, detail))
            _formState.value = _formState.value.copy(reasonLinks = links)
        }
    }

    fun removeReason(reasonId: Long) {
        _formState.value = _formState.value.copy(reasonLinks = _formState.value.reasonLinks.filter { it.reasonId != reasonId })
    }

    fun updateReasonDetail(reasonId: Long, detail: String?) {
        _formState.value = _formState.value.copy(
            reasonLinks = _formState.value.reasonLinks.map { if (it.reasonId == reasonId) it.copy(detail = detail) else it }
        )
    }

    suspend fun save() {
        val form = _formState.value
        require(form.reasonLinks.isNotEmpty()) { "至少选择一个错因" }
        saveQuestionUseCase(
            imagePath = form.ocrText?.let { it } ?: "",
            ocrText = form.ocrText,
            note = form.note,
            source = form.source,
            subjectId = form.subjectId,
            reasonLinks = form.reasonLinks.map { it.reasonId to it.detail },
        )
    }

    fun reset() {
        _uiState.value = CaptureUiState.Ready(null)
        _formState.value = CaptureFormState()
    }
}