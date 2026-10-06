package com.errorbook.app.ui.reasonmanage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errorbook.app.R
import com.errorbook.app.data.local.entity.ReasonEntity
import com.errorbook.app.ui.components.ErrorBookTopBar
import com.errorbook.app.ui.components.ReasonChip
import com.errorbook.app.ui.theme.NotionShape
import com.errorbook.app.ui.theme.NotionSpacing
import com.errorbook.app.ui.theme.ReasonCategory

@Composable
fun ReasonManageScreen(modifier: Modifier = Modifier, viewModel: ReasonManageViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }

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
        topBar = {
            ErrorBookTopBar(
                title = "错因管理",
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plus),
                            contentDescription = "新建错因",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("搜索错因") },
                shape = NotionShape.extraSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NotionSpacing.md, vertical = NotionSpacing.xs),
                singleLine = true,
            )

            if (uiState.active.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "没有匹配的错因",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = NotionSpacing.md,
                        end = NotionSpacing.md,
                        bottom = NotionSpacing.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(NotionSpacing.sm),
                ) {
                    items(uiState.active, key = { it.id }) { reason ->
                        ReasonRow(
                            reason = reason,
                            onRename = { viewModel.startEdit(reason) },
                            onArchive = { viewModel.setArchived(reason, true) },
                            onDelete = { viewModel.delete(reason) },
                            onMerge = { viewModel.startMerge(reason) },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        NameDialog(
            title = "新建错因",
            label = "错因名称",
            onConfirm = {
                viewModel.add(it, null)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    uiState.editing?.let { target ->
        NameDialog(
            title = "重命名",
            label = "新名称",
            initial = target.name,
            onConfirm = {
                viewModel.rename(target, it)
            },
            onDismiss = viewModel::cancelEdit,
        )
    }

    uiState.mergeSource?.let { source ->
        AlertDialog(
            onDismissRequest = viewModel::cancelMerge,
            title = { Text("把「${source.name}」合并到") },
            text = {
                LazyColumn {
                    items(uiState.active.filter { it.id != source.id }, key = { it.id }) { target ->
                        ReasonChip(
                            name = target.name,
                            onClick = { viewModel.mergeInto(target) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = NotionSpacing.xxs),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = viewModel::cancelMerge) { Text("取消") } },
        )
    }
}

@Composable
private fun ReasonRow(reason: ReasonEntity, onRename: () -> Unit, onArchive: () -> Unit, onDelete: () -> Unit, onMerge: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NotionSpacing.xxs),
        verticalArrangement = Arrangement.spacedBy(NotionSpacing.xs),
    ) {
        ReasonChip(
            name = reason.name,
            categoryColor = ReasonCategory.fromLabel(reason.category).color,
            trailing = reason.category,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NotionSpacing.xs),
        ) {
            SmallAction(R.drawable.ic_pencil, "重命名", onRename)
            SmallAction(R.drawable.ic_archive, "归档", onArchive)
            SmallAction(R.drawable.ic_check_check, "合并", onMerge)
            SmallAction(R.drawable.ic_trash_2, "删除", onDelete)
        }
    }
}

@Composable
private fun SmallAction(iconRes: Int, label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = NotionSpacing.xxs),
        )
    }
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
