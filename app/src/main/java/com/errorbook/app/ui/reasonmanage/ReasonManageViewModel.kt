package com.errorbook.app.ui.reasonmanage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.errorbook.app.data.local.entity.ReasonEntity
import com.errorbook.app.data.repository.ReasonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReasonManageUiState(
    val isLoading: Boolean = true,
    val active: List<ReasonEntity> = emptyList(),
    val archived: List<ReasonEntity> = emptyList(),
    val query: String = "",
    val editing: ReasonEntity? = null,
    val mergeSource: ReasonEntity? = null,
    val message: String? = null,
)

/** 错因管理：新增 / 重命名 / 归档 / 合并。防重复靠相似搜索提示（PRD §5.2.3）。 */
@HiltViewModel
class ReasonManageViewModel @Inject constructor(
    private val reasonRepository: ReasonRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val editing = MutableStateFlow<ReasonEntity?>(null)
    private val mergeSource = MutableStateFlow<ReasonEntity?>(null)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ReasonManageUiState> = combine(
        reasonRepository.getActive(),
        query,
        editing,
        mergeSource,
        message,
    ) { active, q, edit, merge, msg ->
        val filtered = if (q.isBlank()) active else active.filter { it.name.contains(q.trim()) }
        ReasonManageUiState(
            isLoading = false,
            active = filtered,
            archived = emptyList(),
            query = q,
            editing = edit,
            mergeSource = merge,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReasonManageUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun startEdit(reason: ReasonEntity) {
        editing.value = reason
    }

    fun cancelEdit() {
        editing.value = null
    }

    fun rename(reason: ReasonEntity, newName: String) {
        viewModelScope.launch {
            reasonRepository.rename(reason, newName)
            editing.value = null
        }
    }

    fun add(name: String, category: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching { reasonRepository.add(trimmed, category, null) }
                .onSuccess { message.value = "已创建「$trimmed」" }
                .onFailure { message.value = "创建失败：名称可能重复" }
        }
    }

    fun setArchived(reason: ReasonEntity, archived: Boolean) {
        viewModelScope.launch { reasonRepository.setArchived(reason, archived) }
    }

    fun delete(reason: ReasonEntity) {
        viewModelScope.launch { reasonRepository.delete(reason) }
    }

    fun startMerge(reason: ReasonEntity) {
        mergeSource.value = reason
    }

    fun cancelMerge() {
        mergeSource.value = null
    }

    fun mergeInto(target: ReasonEntity) {
        val source = mergeSource.value ?: return
        viewModelScope.launch {
            reasonRepository.merge(source.id, target.id)
            mergeSource.value = null
            message.value = "已把「${source.name}」合并到「${target.name}」"
        }
    }

    fun clearMessage() {
        message.value = null
    }
}
