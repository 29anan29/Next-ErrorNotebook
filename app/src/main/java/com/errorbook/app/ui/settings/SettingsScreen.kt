package com.errorbook.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errorbook.app.BuildConfig
import com.errorbook.app.data.local.entity.SubjectEntity
import com.errorbook.app.ui.components.ErrorBookTopBar
import com.errorbook.app.ui.theme.NotionShape
import com.errorbook.app.ui.theme.NotionSpacing

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<SubjectEntity?>(null) }
    var deleting by remember { mutableStateOf<SubjectEntity?>(null) }

    val dirPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> viewModel.onExportDirPicked(uri) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ErrorBookTopBar(title = "设置") },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(NotionSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NotionSpacing.md),
        ) {
            item { SectionTitle("科目管理") }

            if (uiState.subjects.isEmpty()) {
                item {
                    Text(
                        "还没有科目，录题时可以先归到「未分类」。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(uiState.subjects, key = { it.id }) { subject ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(subject.name, style = MaterialTheme.typography.bodyLarge)
                        Row {
                            TextButton(onClick = { renaming = subject }) { Text("重命名") }
                            TextButton(onClick = { deleting = subject }) { Text("删除") }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = NotionShape.full,
                ) {
                    Text("新增科目")
                }
            }

            item { HorizontalDivider() }
            item { SectionTitle("默认导出目录") }
            item {
                Text(
                    text = if (uiState.defaultExportDir == null) {
                        "尚未设置，导出时会每次询问。"
                    } else {
                        "已设置，导出时直接写入该目录。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                OutlinedButton(
                    onClick = { dirPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = NotionShape.full,
                ) {
                    Text(
                        if (uiState.defaultExportDir == null) "选择目录" else "更换目录",
                    )
                }
            }

            item { HorizontalDivider() }
            item { SectionTitle("关于") }
            item {
                Text("版本 ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                Text(
                    "所有数据仅保存在本机，应用不联网，也没有申请网络权限。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Text(
                    "本应用运行于 HarmonyOS 4.x 及以下。HarmonyOS NEXT / 5.0+ 已移除\n" +
                        "AOSP 兼容层，无法安装 APK。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Text(
                    "若安装被拦截，请在系统设置中关闭「纯净模式」，或允许本应用安装未知来源应用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showAddDialog) {
        NameDialog(
            title = "新增科目",
            label = "科目名称",
            onConfirm = {
                viewModel.addSubject(it)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    renaming?.let { target ->
        NameDialog(
            title = "重命名科目",
            label = "新名称",
            initial = target.name,
            onConfirm = {
                viewModel.renameSubject(target, it)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }

    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除科目「${target.name}」？") },
            text = { Text("其下的题目会变为未分类。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSubject(target)
                    deleting = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = NotionSpacing.xs),
    )
}

@Composable
private fun NameDialog(title: String, label: String, initial: String = "", onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                shape = NotionShape.extraSmall,
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
