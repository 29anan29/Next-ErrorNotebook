package com.errorbook.app.ui.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.errorbook.app.data.local.entity.ReasonEntity
import com.errorbook.app.data.local.entity.SubjectEntity
import com.errorbook.app.data.repository.ReasonRepository
import com.errorbook.app.data.repository.SubjectRepository
import com.errorbook.app.domain.usecase.RecognizeTextUseCase
import com.errorbook.app.domain.usecase.SaveQuestionUseCase
import com.errorbook.app.platform.camera.ImageStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 录题页三个阶段：等待选图 → 处理中（压缩 + OCR）→ 编辑。 */
enum class CaptureStep { IDLE, PROCESSING, EDITING }

data class ReasonSelection(
    val reason: ReasonEntity,
    val detail: String? = null,
)

data class CaptureUiState(
    val step: CaptureStep = CaptureStep.IDLE,
    val imagePath: String? = null,
    val ocrText: String = "",
    val note: String = "",
    val source: String = "",
    val subjectId: Long? = null,
    val reasons: List<ReasonSelection> = emptyList(),
    val recentReasons: List<ReasonEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val similarCandidates: List<ReasonEntity> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSave: Boolean get() = reasons.isNotEmpty() && imagePath != null && !isSaving
    val canStartOver: Boolean get() = step == CaptureStep.EDITING && !isSaving
}

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val imageStore: ImageStore,
    private val recognizeTextUseCase: RecognizeTextUseCase,
    private val saveQuestionUseCase: SaveQuestionUseCase,
    private val reasonRepository: ReasonRepository,
    subjectRepository: SubjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState

    /** 录题时展示最近高频错因（PRD §5.2.2），点选即关联。 */
    val recentReasons: StateFlow<List<ReasonEntity>> = reasonRepository.getRecentUsed(RECENT_LIMIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            subjectRepository.getAll().collect { subjects ->
                _uiState.value = _uiState.value.copy(subjects = subjects)
            }
        }
    }

    /** 相册选图。 */
    fun onGalleryImagePicked(uri: Uri) = processImage { imageStore.importFromUri(uri) }

    /** 相机拍照后的沙箱临时文件（FileProvider 授权的 content:// uri）。 */
    fun onPhotoCaptured(uri: Uri) = processImage { imageStore.importCapture(uri) }

    private fun processImage(import: suspend () -> String) {
        _uiState.value = _uiState.value.copy(step = CaptureStep.PROCESSING, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                val path = import()
                // OCR 失败不阻断录题：降级为手动输入，原图仍然保存。
                val ocr = recognizeTextUseCase(Uri.fromFile(File(path))).getOrNull().orEmpty()
                path to ocr
            }.fold(
                onSuccess = { (path, ocr) ->
                    _uiState.value = _uiState.value.copy(
                        step = CaptureStep.EDITING,
                        imagePath = path,
                        ocrText = ocr,
                        errorMessage = if (ocr.isBlank()) "未识别到文字，可手动输入" else null,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        step = CaptureStep.IDLE,
                        errorMessage = e.message ?: "图片处理失败",
                    )
                },
            )
        }
    }

    fun updateOcrText(text: String) {
        _uiState.value = _uiState.value.copy(ocrText = text)
    }

    fun updateNote(text: String) {
        _uiState.value = _uiState.value.copy(note = text)
    }

    fun updateSource(text: String) {
        _uiState.value = _uiState.value.copy(source = text)
    }

    fun selectSubject(id: Long?) {
        _uiState.value = _uiState.value.copy(subjectId = id)
    }

    fun toggleReason(reason: ReasonEntity) {
        val current = _uiState.value.reasons
        val exists = current.any { it.reason.id == reason.id }
        _uiState.value = _uiState.value.copy(
            reasons = if (exists) {
                current.filterNot { it.reason.id == reason.id }
            } else {
                current + ReasonSelection(reason)
            },
        )
    }

    fun updateReasonDetail(reasonId: Long, detail: String?) {
        _uiState.value = _uiState.value.copy(
            reasons = _uiState.value.reasons.map {
                if (it.reason.id == reasonId) it.copy(detail = detail?.takeIf { d -> d.isNotBlank() }) else it
            },
        )
    }

    /** 新建错因：先搜相似项提示合并，避免错因重复导致统计混乱（PRD §5.2.3）。 */
    fun createReason(name: String, category: String? = null, color: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val similar = reasonRepository.searchSimilar(trimmed)
            if (similar.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(similarCandidates = similar)
                return@launch
            }
            val id = reasonRepository.add(trimmed, category, color)
            val created = ReasonEntity(
                id = id,
                name = trimmed,
                category = category,
                color = color,
                createdAt = System.currentTimeMillis(),
            )
            _uiState.value = _uiState.value.copy(
                reasons = _uiState.value.reasons + ReasonSelection(created),
                similarCandidates = emptyList(),
            )
        }
    }

    fun dismissSimilarCandidates() {
        _uiState.value = _uiState.value.copy(similarCandidates = emptyList())
    }

    /** 合并到已有错因：把新错因名下的关联迁回目标错因并删除新错因。 */
    fun mergeInto(existing: ReasonEntity, from: ReasonEntity) {
        viewModelScope.launch {
            reasonRepository.merge(from.id, existing.id)
            _uiState.value = _uiState.value.copy(
                reasons = _uiState.value.reasons.mapNotNull {
                    when {
                        it.reason.id == from.id -> ReasonSelection(existing)
                        else -> it
                    }
                },
                similarCandidates = emptyList(),
            )
        }
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val path = state.imagePath ?: return
        if (state.reasons.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "至少选择一个错因")
            return
        }
        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                saveQuestionUseCase(
                    imagePath = path,
                    ocrText = state.ocrText,
                    note = state.note,
                    source = state.source,
                    subjectId = state.subjectId,
                    reasonLinks = state.reasons.map { it.reason.id to it.detail },
                )
            }.fold(
                onSuccess = {
                    _uiState.value = CaptureUiState()
                    onSaved()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "保存失败",
                    )
                },
            )
        }
    }

    fun reset() {
        _uiState.value = CaptureUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private companion object {
        const val RECENT_LIMIT = 12
    }
}
