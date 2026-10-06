package com.errorbook.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.errorbook.app.data.local.SettingsDataStore
import com.errorbook.app.data.local.entity.SubjectEntity
import com.errorbook.app.data.repository.SubjectRepository
import com.errorbook.app.platform.file.FileSaveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val subjects: List<SubjectEntity> = emptyList(),
    val defaultExportDir: Uri? = null,
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val fileSaveManager: FileSaveManager,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val defaultDir = MutableStateFlow<Uri?>(null)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        subjectRepository.getAll(),
        defaultDir,
        message,
    ) { subjects, dir, msg ->
        SettingsUiState(isLoading = false, subjects = subjects, defaultExportDir = dir, message = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        viewModelScope.launch {
            defaultDir.value = settingsDataStore.defaultExportDir.first()?.let(Uri::parse)
        }
    }

    fun addSubject(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching { subjectRepository.add(trimmed) }
                .onSuccess { message.value = "已新增「$trimmed」" }
                .onFailure { message.value = "新增失败：科目名可能重复" }
        }
    }

    fun renameSubject(subject: SubjectEntity, newName: String) {
        viewModelScope.launch {
            subjectRepository.rename(subject, newName)
            message.value = "已重命名"
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            subjectRepository.delete(subject)
            message.value = "已删除「${subject.name}」"
        }
    }

    fun onExportDirPicked(uri: Uri?) {
        if (uri == null) return
        fileSaveManager.persistPermission(uri)
        defaultDir.value = uri
        viewModelScope.launch { settingsDataStore.setDefaultExportDir(uri.toString()) }
        message.value = "已设置默认导出目录"
    }

    fun clearMessage() {
        message.value = null
    }
}
