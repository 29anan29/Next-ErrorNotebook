package com.errorbook.app.ui.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.errorbook.app.ui.components.ErrorBookTopBar
import com.errorbook.app.ui.components.ReasonChip
import com.errorbook.app.ui.theme.NotionShape
import com.errorbook.app.ui.theme.NotionSpacing
import com.errorbook.app.ui.theme.ReasonCategory
import java.io.File

@Composable
fun CaptureScreen(onSaved: () -> Unit, modifier: Modifier = Modifier, viewModel: CaptureViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var newReasonName by remember { mutableStateOf("") }
    var showNewReasonDialog by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::onGalleryImagePicked) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) viewModel.onPhotoCaptured(uri) else viewModel.reset()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createCameraUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ErrorBookTopBar(
                title = "录题",
                actions = {
                    if (uiState.canStartOver) {
                        TextButton(onClick = viewModel::reset) { Text("重录") }
                    }
                },
            )
        },
    ) { padding ->
        when (uiState.step) {
            CaptureStep.IDLE -> IdlePane(
                modifier = Modifier.padding(padding),
                onTakePhoto = {
                    if (hasCameraPermission(context)) {
                        val uri = createCameraUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onPickGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )

            CaptureStep.PROCESSING -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(NotionSpacing.md),
                ) {
                    CircularProgressIndicator()
                    Text("正在压缩并识别文字…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            CaptureStep.EDITING -> EditPane(
                uiState = uiState,
                modifier = Modifier.padding(padding),
                onOcrTextChange = viewModel::updateOcrText,
                onNoteChange = viewModel::updateNote,
                onSourceChange = viewModel::updateSource,
                onSubjectSelected = viewModel::selectSubject,
                onToggleReason = viewModel::toggleReason,
                onReasonDetailChange = viewModel::updateReasonDetail,
                onSave = { viewModel.save(onSaved) },
                onOpenNewReason = { showNewReasonDialog = true },
            )
        }
    }

    if (showNewReasonDialog) {
        NewReasonDialog(
            name = newReasonName,
            onNameChange = { newReasonName = it },
            onConfirm = {
                viewModel.createReason(newReasonName)
                newReasonName = ""
                showNewReasonDialog = false
            },
            onDismiss = {
                newReasonName = ""
                showNewReasonDialog = false
            },
        )
    }

    val candidates = uiState.similarCandidates
    if (candidates.isNotEmpty()) {
        SimilarReasonDialog(
            candidates = candidates,
            onPick = { existing ->
                val pending = candidates.first()
                viewModel.mergeInto(existing, pending)
            },
            onDismiss = viewModel::dismissSimilarCandidates,
        )
    }
}

@Composable
private fun IdlePane(modifier: Modifier, onTakePhoto: () -> Unit, onPickGallery: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(NotionSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NotionSpacing.md),
    ) {
        Text(
            "只记错因，不记答案",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "拍下题目后自动识别文字，你只需要想清楚「我为什么错」，再选错因。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth(),
            shape = NotionShape.full,
        ) {
            Text("拍照", style = MaterialTheme.typography.labelLarge)
        }
        OutlinedButton(
            onClick = onPickGallery,
            modifier = Modifier.fillMaxWidth(),
            shape = NotionShape.full,
        ) {
            Text("从相册选择", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditPane(uiState: CaptureUiState, modifier: Modifier, onOcrTextChange: (String) -> Unit, onNoteChange: (String) -> Unit, onSourceChange: (String) -> Unit, onSubjectSelected: (Long?) -> Unit, onToggleReason: (com.errorbook.app.data.local.entity.ReasonEntity) -> Unit, onReasonDetailChange: (Long, String?) -> Unit, onSave: () -> Unit, onOpenNewReason: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(NotionSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NotionSpacing.md),
    ) {
        uiState.imagePath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
            )
        }

        OutlinedTextField(
            value = uiState.ocrText,
            onValueChange = onOcrTextChange,
            label = { Text("题目文本") },
            shape = NotionShape.extraSmall,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 8,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(NotionSpacing.sm)) {
            OutlinedTextField(
                value = uiState.source,
                onValueChange = onSourceChange,
                label = { Text("来源") },
                shape = NotionShape.extraSmall,
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            SubjectDropdown(
                subjects = uiState.subjects,
                selectedId = uiState.subjectId,
                onSelected = onSubjectSelected,
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = uiState.note,
            onValueChange = onNoteChange,
            label = { Text("备注（可选）") },
            shape = NotionShape.extraSmall,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
        )

        Text("为什么错？（至少选一个）", style = MaterialTheme.typography.titleMedium)
        Text(
            "先想清楚再选，选了默认是这个错因；下方可以补一句具体说明。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NotionSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(NotionSpacing.xs),
        ) {
            uiState.recentReasons.forEach { reason ->
                val selection = uiState.reasons.firstOrNull { it.reason.id == reason.id }
                ReasonChip(
                    name = reason.name,
                    selected = selection != null,
                    categoryColor = ReasonCategory.fromLabel(reason.category).color,
                    onClick = { onToggleReason(reason) },
                )
            }
            OutlinedButton(
                onClick = onOpenNewReason,
                shape = NotionShape.full,
                modifier = Modifier.size(width = 88.dp, height = 32.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = NotionSpacing.sm,
                ),
            ) {
                Text("+ 新建", style = MaterialTheme.typography.bodySmall)
            }
        }

        uiState.reasons.forEach { selection ->
            OutlinedTextField(
                value = selection.detail.orEmpty(),
                onValueChange = { onReasonDetailChange(selection.reason.id, it) },
                label = { Text("${selection.reason.name}：具体错因说明（可选）") },
                placeholder = { Text("例如「符号看错」") },
                shape = NotionShape.extraSmall,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        Button(
            onClick = onSave,
            enabled = uiState.canSave,
            modifier = Modifier.fillMaxWidth(),
            shape = NotionShape.full,
        ) {
            Text(
                text = if (uiState.isSaving) "保存中…" else "保存",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SubjectDropdown(subjects: List<com.errorbook.app.data.local.entity.SubjectEntity>, selectedId: Long?, onSelected: (Long?) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val label = subjects.firstOrNull { it.id == selectedId }?.name ?: "未分类"
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("未分类") },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            subjects.forEach { subject ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(subject.name) },
                    onClick = {
                        onSelected(subject.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun NewReasonDialog(name: String, onNameChange: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建错因") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("错因名称") },
                shape = NotionShape.extraSmall,
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("创建") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun SimilarReasonDialog(candidates: List<com.errorbook.app.data.local.entity.ReasonEntity>, onPick: (com.errorbook.app.data.local.entity.ReasonEntity) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("已存在相近的错因") },
        text = {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(NotionSpacing.xs)) {
                items(candidates, key = { it.id }) { candidate ->
                    ReasonChip(
                        name = candidate.name,
                        onClick = { onPick(candidate) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

private fun hasCameraPermission(context: Context): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
    PackageManager.PERMISSION_GRANTED

/** 相机输出走 FileProvider 授权的沙箱临时文件，Capture 完成后会被搬进 images/。 */
private fun createCameraUri(context: Context) = com.errorbook.app.platform.camera.CameraOutput.createUri(context)
