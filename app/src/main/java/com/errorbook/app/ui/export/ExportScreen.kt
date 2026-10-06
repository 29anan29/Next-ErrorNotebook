package com.errorbook.app.ui.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errorbook.app.ui.components.EmptyState
import com.errorbook.app.ui.components.ErrorBookTopBar
import com.errorbook.app.ui.components.QuestionThumbnail
import com.errorbook.app.ui.theme.NotionShape
import com.errorbook.app.ui.theme.NotionSpacing

@Composable
fun ExportScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ExportViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

    val dirPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> viewModel.onDirectoryPicked(uri) }

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
            ErrorBookTopBar(title = "导出错因清单", onBack = onBack)
        },
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (uiState.entries.isEmpty()) {
            EmptyState(
                title = "没有可导出的错因",
                body = "先录几道错题，排行榜有内容后就能导出。",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = NotionSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NotionSpacing.md),
        ) {
            Text("导出范围", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(NotionSpacing.xs)) {
                items(ExportUiState.TOP_N_OPTIONS) { n ->
                    FilterChip(
                        selected = uiState.topN == n,
                        onClick = { viewModel.setTopN(n) },
                        label = { Text("Top $n") },
                    )
                }
            }

            Button(
                onClick = { dirPicker.launch(null) },
                modifier = Modifier.fillMaxWidth(),
                shape = NotionShape.full,
            ) {
                Text(
                    text = if (uiState.defaultDirUri == null) "选择保存目录" else "更换保存目录",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (uiState.defaultDirUri != null) {
                Text(
                    text = "已设置默认目录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text("预览", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = NotionSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(NotionSpacing.sm),
            ) {
                items(uiState.entries, key = { it.reasonName }) { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(NotionSpacing.xs)) {
                        Text(
                            text = "${entry.reasonName}（${entry.wrongCount} 次 · ${entry.questionCount} 题）",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(NotionSpacing.xs)) {
                            items(entry.thumbnailPaths) { path ->
                                QuestionThumbnail(
                                    imagePath = path,
                                    modifier = Modifier
                                        .fillMaxWidth(0.28f)
                                        .height(96.dp),
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = viewModel::export,
                enabled = uiState.canExport,
                modifier = Modifier.fillMaxWidth(),
                shape = NotionShape.full,
            ) {
                Text(
                    text = if (uiState.isExporting) "导出中…" else "导出 PDF",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
