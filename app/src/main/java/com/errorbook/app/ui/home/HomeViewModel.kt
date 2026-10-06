package com.errorbook.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.errorbook.app.data.local.entity.SubjectEntity
import com.errorbook.app.data.model.ReasonWithCount
import com.errorbook.app.data.repository.SubjectRepository
import com.errorbook.app.domain.usecase.GetReasonRankingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class TimeRange(val label: String) {
    ALL("全部"),
    LAST_7_DAYS("近 7 天"),
    LAST_30_DAYS("近 30 天"),
    ;

    fun sinceMillis(now: Long): Long? = when (this) {
        ALL -> null
        LAST_7_DAYS -> now - TimeUnit.DAYS.toMillis(7)
        LAST_30_DAYS -> now - TimeUnit.DAYS.toMillis(30)
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val reasons: List<ReasonWithCount> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val selectedSubjectId: Long? = null,
    val timeRange: TimeRange = TimeRange.ALL,
) {
    val isEmpty: Boolean get() = !isLoading && reasons.isEmpty()
}

/**
 * 首页 = 错因排行榜。筛选状态与查询合并成一条 Flow：
 * 科目与时间范围任意组合都交给同一条 DAO 查询处理，
 * 避免文档示例里「切到时间筛选就丢掉科目筛选」的问题。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getReasonRankingUseCase: GetReasonRankingUseCase,
    subjectRepository: SubjectRepository,
) : ViewModel() {

    private val selectedSubject = MutableStateFlow<Long?>(null)
    private val timeRange = MutableStateFlow(TimeRange.ALL)

    val uiState: StateFlow<HomeUiState> = combine(
        combine(selectedSubject, timeRange, ::Pair),
        subjectRepository.getAll(),
    ) { (subjectId, range), subjects ->
        Triple(subjectId, range, subjects)
    }.flatMapLatest { (subjectId, range, subjects) ->
        getReasonRankingUseCase(subjectId, range.sinceMillis(System.currentTimeMillis()))
            .map { ranking ->
                HomeUiState(
                    isLoading = false,
                    reasons = ranking,
                    subjects = subjects,
                    selectedSubjectId = subjectId,
                    timeRange = range,
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun selectSubject(subjectId: Long?) {
        selectedSubject.value = subjectId
    }

    fun selectTimeRange(range: TimeRange) {
        timeRange.value = range
    }
}
