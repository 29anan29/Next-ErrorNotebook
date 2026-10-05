package com.errorbook.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.errorbook.app.data.model.ReasonWithCount
import com.errorbook.app.domain.usecase.GetReasonRankingUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.Nullable
import java.util.concurrent.TimeUnit

data class HomeUiState(
    val isLoading: Boolean = true,
    val reasons: List<ReasonWithCount> = emptyList(),
    val selectedSubjectId: Long? = null,
    val timeRange: TimeRange = TimeRange.ALL,
)

enum class TimeRange { ALL, LAST_7_DAYS, LAST_30_DAYS }

class HomeViewModel @Inject constructor(
    private val getReasonRankingUseCase: GetReasonRankingUseCase,
) : ViewModel() {

    private val _selectedSubject = mutableStateFlow<Long?>(null)
    private val _timeRange = mutableStateFlow(TimeRange.ALL)

    val uiState: Flow<HomeUiState> = combine(_selectedSubject, _timeRange) { subjectId, range ->
        subjectId to range
    }.flatMapLatest { (subjectId, range) ->
        val since = when (range) {
            TimeRange.ALL -> null
            TimeRange.LAST_7_DAYS -> System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            TimeRange.LAST_30_DAYS -> System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        }
        getReasonRankingUseCase(subjectId, since)
    }.map { reasons ->
        HomeUiState(isLoading = false, reasons = reasons)
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun selectSubject(@Nullable subjectId: Long?) { _selectedSubject.value = subjectId }
    fun selectTimeRange(range: TimeRange) { _timeRange.value = range }
}