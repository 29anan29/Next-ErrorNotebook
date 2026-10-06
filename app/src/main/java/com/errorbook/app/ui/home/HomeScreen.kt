package com.errorbook.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.errorbook.app.R
import com.errorbook.app.ui.theme.ErrorBookTheme
import com.errorbook.app.ui.theme.NotionSpacing

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onReasonClick: (Long) -> Unit,
    onSubjectSelected: (Long?) -> Unit,
    onTimeRangeSelected: (HomeViewModel.TimeRange) -> Unit,
    onExportClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(NotionSpacing.md),
    ) {
        Header(onExportClick = onExportClick)
        Filters(
            subjectId = uiState.selectedSubjectId,
            timeRange = uiState.timeRange,
            onSubjectSelected = onSubjectSelected,
            onTimeRangeSelected = onTimeRangeSelected,
        )
        ReasonsList(reasons = uiState.reasons, onReasonClick = onReasonClick)
    }
}

@Composable
private fun Header(onExportClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(NotionSpacing.md),
    ) {
        Text("错因排行榜", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Filters(
    subjectId: Long?,
    timeRange: HomeViewModel.TimeRange,
    onSubjectSelected: (Long?) -> Unit,
    onTimeRangeSelected: (HomeViewModel.TimeRange) -> Unit,
) {
    var expandedSubject by remember { mutableStateOf(false) }
    var expandedRange by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NotionSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NotionSpacing.xs),
    ) {
        FilterMenu(
            label = "全部科目",
            expanded = expandedSubject,
            onExpandChange = { expandedSubject = it },
            menuContent = {
                DropdownMenuItem(text = { Text("全部科目") }, onClick = { onSubjectSelected(null); expandedSubject = false })
            },
        )
        FilterMenu(
            label = timeRange.label,
            expanded = expandedRange,
            onExpandChange = { expandedRange = it },
            menuContent = {
                HomeViewModel.TimeRange.values().forEach { range ->
                    DropdownMenuItem(
                        text = { Text(range.label) },
                        onClick = { onTimeRangeSelected(range); expandedRange = false },
                    )
                }
            },
        )
    }
}

@Composable
private fun FilterMenu(
    label: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    menuContent: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NotionSpacing.xxs),
    ) {
        var menuExpanded by remember { mutableStateOf(expanded) }
        if (menuExpanded != expanded) menuExpanded = expanded

        Button(
            onClick = { menuExpanded = !menuExpanded; onExpandChange(menuExpanded) },
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.filledButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false; onExpandChange(false) },
        ) {
            menuContent()
        }
    }
}

@Composable
private fun ReasonsList(reasons: List<ReasonWithCount>, onReasonClick: (Long) -> Unit) {
    if (reasons.isEmpty()) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Text(
                "还没有错因记录\n拍下第一道错题并选择错因，这里就会开始累计频次",
                textAlign = androidx.compose.ui.text.TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = NotionSpacing.md, vertical = NotionSpacing.sm),
            contentPadding = NotionSpacing.md.toPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(NotionSpacing.sm),
        ) {
            items(reasons, key = { it.id }) { reason ->
                ReasonRow(reason = reason, onClick = { onReasonClick(reason.id) })
            }
        }
    }
}

@Composable
private fun ReasonRow(reason: ReasonWithCount, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(NotionSpacing.xxs),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        onClick = onClick,
    ) {
        androidx.compose.foundation.layout.Row(
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
                Text(reason.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${reason.wrongCount} 次 · ${reason.questionCount} 题", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.material3.Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

private val HomeViewModel.TimeRange.label: String
    get() = when (this) {
        HomeViewModel.TimeRange.ALL -> "全部"
        HomeViewModel.TimeRange.LAST_7_DAYS -> "近 7 天"
        HomeViewModel.TimeRange.LAST_30_DAYS -> "近 30 天"
    }