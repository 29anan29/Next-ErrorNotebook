package com.errorbook.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errorbook.app.R
import com.errorbook.app.data.model.ReasonWithCount
import com.errorbook.app.ui.components.EmptyState
import com.errorbook.app.ui.components.ErrorBookTopBar
import com.errorbook.app.ui.theme.NotionSpacing

@Composable
fun HomeScreen(onReasonClick: (Long) -> Unit, onExportClick: () -> Unit, modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        uiState = uiState,
        onReasonClick = onReasonClick,
        onExportClick = onExportClick,
        onSubjectSelected = viewModel::selectSubject,
        onTimeRangeSelected = viewModel::selectTimeRange,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(uiState: HomeUiState, onReasonClick: (Long) -> Unit, onExportClick: () -> Unit, onSubjectSelected: (Long?) -> Unit, onTimeRangeSelected: (TimeRange) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ErrorBookTopBar(
                title = "错因排行榜",
                subtitle = "按出错次数降序，考前只看这里",
                actions = {
                    IconButton(onClick = onExportClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share_2),
                            contentDescription = "导出",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            FilterRow(
                selectedSubjectId = uiState.selectedSubjectId,
                subjects = uiState.subjects,
                timeRange = uiState.timeRange,
                onSubjectSelected = onSubjectSelected,
                onTimeRangeSelected = onTimeRangeSelected,
            )

            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                uiState.isEmpty -> EmptyState(
                    title = "还没有错因记录",
                    body = "拍下第一道错题并选择错因，这里就会开始累计频次。",
                    iconRes = R.drawable.ic_lightbulb,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = NotionSpacing.md,
                        end = NotionSpacing.md,
                        bottom = NotionSpacing.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(NotionSpacing.sm),
                ) {
                    items(uiState.reasons, key = { it.id }) { reason ->
                        ReasonRow(reason = reason, onClick = { onReasonClick(reason.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(selectedSubjectId: Long?, subjects: List<com.errorbook.app.data.local.entity.SubjectEntity>, timeRange: TimeRange, onSubjectSelected: (Long?) -> Unit, onTimeRangeSelected: (TimeRange) -> Unit) {
    var subjectMenuOpen by remember { mutableStateOf(false) }
    var rangeMenuOpen by remember { mutableStateOf(false) }

    val selectedSubjectName = subjects.firstOrNull { it.id == selectedSubjectId }?.name ?: "全部科目"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NotionSpacing.md, vertical = NotionSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(NotionSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            TextButton(onClick = { subjectMenuOpen = true }) {
                Text(selectedSubjectName, style = MaterialTheme.typography.bodyMedium)
            }
            DropdownMenu(expanded = subjectMenuOpen, onDismissRequest = { subjectMenuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("全部科目") },
                    onClick = {
                        onSubjectSelected(null)
                        subjectMenuOpen = false
                    },
                )
                subjects.forEach { subject ->
                    DropdownMenuItem(
                        text = { Text(subject.name) },
                        onClick = {
                            onSubjectSelected(subject.id)
                            subjectMenuOpen = false
                        },
                    )
                }
            }
        }

        Box {
            TextButton(onClick = { rangeMenuOpen = true }) {
                Text(timeRange.label, style = MaterialTheme.typography.bodyMedium)
            }
            DropdownMenu(expanded = rangeMenuOpen, onDismissRequest = { rangeMenuOpen = false }) {
                TimeRange.entries.forEach { range ->
                    DropdownMenuItem(
                        text = { Text(range.label) },
                        onClick = {
                            onTimeRangeSelected(range)
                            rangeMenuOpen = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReasonRow(reason: ReasonWithCount, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NotionSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NotionSpacing.xxs),
            ) {
                Text(
                    text = reason.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    // 同时给出「出错次数」与「关联题目数」——PRD §5.4.1 要求两者都展示
                    text = "错 ${reason.wrongCount} 次 · ${reason.questionCount} 道题",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = NotionSpacing.sm),
            )
        }
    }
}
