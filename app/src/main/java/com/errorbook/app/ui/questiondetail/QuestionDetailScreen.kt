package com.errorbook.app.ui.questiondetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.errorbook.app.ui.components.EmptyState
import com.errorbook.app.ui.components.ErrorBookTopBar
import com.errorbook.app.ui.components.QuestionThumbnail
import com.errorbook.app.ui.components.ReasonChip
import com.errorbook.app.ui.theme.NotionSpacing
import java.io.File

@Composable
fun QuestionDetailScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: QuestionDetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onBack()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ErrorBookTopBar(
                title = "题目详情",
                onBack = onBack,
                actions = {
                    if (uiState.question != null) {
                        TextButton(onClick = { showDeleteDialog = true }) { Text("删除") }
                    }
                },
            )
        },
    ) { padding ->
        val question = uiState.question
        if (question == null) {
            EmptyState(
                title = "题目已不存在",
                body = "它可能已被删除。",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(NotionSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NotionSpacing.md),
        ) {
            if (File(question.imagePath).exists()) {
                AsyncImage(
                    model = File(question.imagePath),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                QuestionThumbnail(
                    imagePath = question.imagePath,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            question.source?.takeIf { it.isNotBlank() }?.let {
                LabeledText("来源", it)
            }
            question.subjectId?.let { LabeledText("科目", "ID $it") }

            Text(
                text = question.ocrText?.takeIf { it.isNotBlank() } ?: "无识别文本",
                style = MaterialTheme.typography.bodyLarge,
            )

            question.note?.takeIf { it.isNotBlank() }?.let {
                LabeledText("备注", it)
            }

            Text("错因", style = MaterialTheme.typography.titleMedium)
            if (uiState.reasons.isEmpty()) {
                Text(
                    "这道题还没有关联错因",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                uiState.reasons.forEach { display ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        ReasonChip(
                            name = display.reasonName,
                            trailing = display.detail,
                            categoryColor = null,
                        )
                        TextButton(onClick = { viewModel.removeReason(display.questionReasonId) }) {
                            Text("移除", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除这道错题？") },
            text = { Text("相关错因频次会同步减少，此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun LabeledText(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(NotionSpacing.xxs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
