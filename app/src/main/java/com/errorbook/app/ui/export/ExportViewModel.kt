package com.errorbook.app.ui.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.errorbook.app.data.local.SettingsDataStore
import com.errorbook.app.data.model.ExportEntry
import com.errorbook.app.data.model.ReasonWithCount
import com.errorbook.app.data.repository.QuestionRepository
import com.errorbook.app.data.repository.ReasonRepository
import com.errorbook.app.domain.usecase.ExportReasonListUseCase
import com.errorbook.app.domain.util.FileNameGenerator
import com.errorbook.app.platform.file.FileSaveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExportUiState(
    val isLoading: Boolean = true,
    val topN: Int = ExportUiState.DEFAULT_TOP_N,
    val entries: List<ExportEntry> = emptyList(),
    val defaultDirUri: Uri? = null,
    val isExporting: Boolean = false,
    val message: String? = null,
) {
    val canExport: Boolean get() = entries.isNotEmpty() && defaultDirUri != null && !isExporting

    companion object {
        const val DEFAULT_TOP_N = 10
        val TOP_N_OPTIONS = listOf(5, 10, 20)
    }
}

/**
 * 导出预览 + 落到用户指定目录。
 * 预览内容随排行榜实时变化；每个错因取前 5 道代表题的缩略图。
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    reasonRepository: ReasonRepository,
    private val questionRepository: QuestionRepository,
    private val exportReasonListUseCase: ExportReasonListUseCase,
    private val fileSaveManager: FileSaveManager,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val topN = MutableStateFlow(ExportUiState.DEFAULT_TOP_N)
    private val defaultDir = MutableStateFlow<Uri?>(null)
    private val isExporting = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private data class RankingInputs(
        val ranking: List<ReasonWithCount>,
        val thumbnails: Map<Long, List<String>>,
    )

    /** 错因 id → 代表题缩略图路径。 */
    private val thumbnails = MutableStateFlow<Map<Long, List<String>>>(emptyMap())

    private val inputs: StateFlow<RankingInputs> = combine(
        reasonRepository.getRanking(null, null),
        thumbnails,
    ) { ranking: List<ReasonWithCount>, thumbs: Map<Long, List<String>> ->
        RankingInputs(ranking, thumbs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RankingInputs(emptyList(), emptyMap()))

    val uiState: StateFlow<ExportUiState> = combine(
        inputs,
        topN,
        defaultDir,
        isExporting,
        message,
    ) { input: RankingInputs, n: Int, dir: Uri?, exporting: Boolean, msg: String? ->
        ExportUiState(
            isLoading = false,
            topN = n,
            entries = input.ranking.take(n).map { reason ->
                ExportEntry(
                    reasonName = reason.name,
                    category = reason.category,
                    wrongCount = reason.wrongCount,
                    questionCount = reason.questionCount,
                    thumbnailPaths = input.thumbnails[reason.id].orEmpty(),
                )
            },
            defaultDirUri = dir,
            isExporting = exporting,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExportUiState())

    init {
        viewModelScope.launch {
            defaultDir.value = settingsDataStore.defaultExportDir.first()?.let(Uri::parse)
        }
        // Top N 变化或录题导致排行榜变化时，重新抓代表题缩略图。
        viewModelScope.launch {
            topN.flatMapLatest { n ->
                reasonRepository.getRanking(null, null).map { ranking -> ranking.take(n).map { it.id } }
            }.collect { reasonIds ->
                thumbnails.value = reasonIds.associateWith { id ->
                    runCatching { questionRepository.getQuestionsForReason(id).first() }
                        .getOrDefault(emptyList())
                        .take(THUMBNAIL_LIMIT)
                        .map { it.imagePath }
                }
            }
        }
    }

    fun setTopN(value: Int) {
        topN.value = value
    }

    /** SAF 目录选择器返回后调用：持久化权限并记为默认导出目录。 */
    fun onDirectoryPicked(uri: Uri?) {
        if (uri == null) return
        fileSaveManager.persistPermission(uri)
        defaultDir.value = uri
        viewModelScope.launch { settingsDataStore.setDefaultExportDir(uri.toString()) }
    }

    fun export() {
        val dir = defaultDir.value
        val entries = uiState.value.entries
        if (dir == null || entries.isEmpty()) return
        isExporting.value = true
        viewModelScope.launch {
            val result = exportReasonListUseCase(
                dirUri = dir,
                fileName = FileNameGenerator.exportPdfName(),
                entries = entries,
            )
            isExporting.value = false
            message.value = result.fold(
                onSuccess = { "已导出到所选目录" },
                onFailure = { e -> e.message ?: "导出失败" },
            )
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private companion object {
        const val THUMBNAIL_LIMIT = 5
    }
}
