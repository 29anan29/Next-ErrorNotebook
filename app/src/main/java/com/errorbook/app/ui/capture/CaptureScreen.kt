package com.errorbook.app.ui.capture

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import com.errorbook.app.ui.theme.NotionShape

@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel = androidx.lifecycle.viewmodel.compose.hiltViewModel(),
    onSaved: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val recentReasons by viewModel.recentReasons.collectAsStateWithLifecycle()

    when (uiState) {
        is CaptureUiState.Ready -> StartScreen(onTakePhoto = { /* camera intent */ }, onPickGallery = { /* gallery intent */ })
        is CaptureUiState.Processing -> ProcessingScreen(imageUri = it.imageUri)
        is CaptureUiState.OcrResult -> EditorScreen(
            formState = formState,
            recentReasons = recentReasons,
            onOcrTextChange = viewModel::updateOcrText,
            onNoteChange = viewModel::updateNote,
            onSourceChange = viewModel::updateSource,
            onSubjectSelect = viewModel::selectSubject,
            onAddReason = viewModel::addReason,
            onRemoveReason = viewModel::removeReason,
            onReasonDetailChange = viewModel::updateReasonDetail,
            onSave = { viewModel.save(); onSaved() },
            onBack = { viewModel.reset() },
        )
        is CaptureUiState.Error -> ErrorScreen(message = it.message, onRetry = { viewModel.reset() })
    }
}

@Composable
private fun StartScreen(
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NotionSpacing.lg)) {
            androidx.compose.material3.Icon(painter = painterResource(R.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(64.dp))
            Text("录入错题", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("拍照或从相册选择一道题目，OCR 自动识别文字", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.material3.Button(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth().padding(NotionSpacing.md)) {
                Text("拍照", style = MaterialTheme.typography.labelLarge)
            }
            androidx.compose.material3.OutlinedButton(onClick = onPickGallery, modifier = Modifier.fillMaxWidth().padding(NotionSpacing.md)) {
                Text("从相册选择", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ProcessingScreen(imageUri: android.net.Uri) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NotionSpacing.md)) {
            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Text("正在识别文字…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EditorScreen(
    formState: CaptureFormState,
    recentReasons: List<com.errorbook.app.data.local.entity.ReasonEntity>,
    onOcrTextChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSourceChange: (String) -> Unit,
    onSubjectSelect: (Long?) -> Unit,
    onAddReason: (Long, String?) -> Unit,
    onRemoveReason: (Long) -> Unit,
    onReasonDetailChange: (Long, String?) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(NotionSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NotionSpacing.md),
    ) {
        // OCR 文本编辑
        TextField(
            value = formState.ocrText,
            onValueChange = onOcrTextChange,
            label = { Text("题目文本") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8,
        )

        // 备注
        TextField(
            value = formState.note,
            onValueChange = onNoteChange,
            label = { Text("备注（可选）") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
        )

        // 来源
        TextField(
            value = formState.source,
            onValueChange = onSourceChange,
            label = { Text("来源（可选，如：期中考试）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // 科目选择（简化）
        androidx.compose.material3.OutlinedButton(
            onClick = { onSubjectSelect(null) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(formState.subjectId?.let { "科目 ID: $it" } ?: "未分类", style = MaterialTheme.typography.bodyMedium)
        }

        // 错因选择
        Text("为什么错？（至少选一个）", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(NotionSpacing.xs)) {
            recentReasons.forEach { reason ->
                ReasonChip(
                    reason = reason,
                    isSelected = formState.reasonLinks.any { it.reasonId == reason.id },
                    onClick = { onAddReason(reason.id, null) },
                    onDetailChange = { detail -> onReasonDetailChange(reason.id, detail) },
                    currentDetail = formState.reasonLinks.firstOrNull { it.reasonId == reason.id }?.detail,
                )
            }
            // 显示已选但不在最近列表的
            formState.reasonLinks.forEach { link ->
                if (recentReasons.none { it.id == link.reasonId }) {
                    ReasonChip(
                        reason = com.errorbook.app.data.local.entity.ReasonEntity(
                            id = link.reasonId,
                            name = "错因 $link.reasonId",
                            category = null,
                            color = null,
                            isArchived = false,
                            createdAt = 0,
                        ),
                        isSelected = true,
                        onClick = { onRemoveReason(link.reasonId) },
                        onDetailChange = { detail -> onReasonDetailChange(link.reasonId, detail) },
                        currentDetail = link.detail,
                    )
                }
            }
        }

        androidx.compose.material3.Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = formState.reasonLinks.isNotEmpty(),
        ) {
            Text("保存", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ReasonChip(
    reason: com.errorbook.app.data.local.entity.ReasonEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDetailChange: (String?) -> Unit,
    currentDetail: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(NotionSpacing.md)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(reason.name, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                androidx.compose.material3.Text(
                    text = currentDetail?.takeIf { it.isNotBlank() } ?: "无说明",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isSelected) {
                TextField(
                    value = currentDetail ?: "",
                    onValueChange = onDetailChange,
                    label = { Text("具体错因说明") },
                    modifier = Modifier.fillMaxWidth().padding(top = NotionSpacing.sm),
                    singleLine = true,
                )
            }
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NotionSpacing.md)) {
            Text("识别失败", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.material3.Button(onClick = onRetry) { Text("重新选择") }
        }
    }
}