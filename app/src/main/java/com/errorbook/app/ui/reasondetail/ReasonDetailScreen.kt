package com.errorbook.app.ui.reasondetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.errorbook.app.data.local.entity.ReviewResult
import com.errorbook.app.ui.components.ErrorBookTopBar
import com.errorbook.app.ui.theme.NotionSpacing
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReasonDetailScreen(
    onBack: () -> Unit,
    onQuestionClick: (Long) -> Unit,
    viewModel: ReasonDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ErrorBookTopBar(
                title = uiState.reason?.name.orEmpty(),
                subtitle = uiState.reason?.let {
                    "${it.wrongCount} 次 · ${it.questionCount} 道题"
                },
                onBack = onBack,
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize())
            uiState.questions.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("这个错因下还没有题目", style = MaterialTheme.typography.bodyMedium)
            }
            else -> ReviewPane(
                uiState = uiState,
                modifier = Modifier.padding(padding),
                onReveal = viewModel::reveal,
                onReview = viewModel::onReviewed,
                onNext = viewModel::next,
                onQuestionClick = onQuestionClick,
            )
        }
    }
}

/** 复习模式：默认只给题目，「显示错因」后才露出错因与说明。 */
@Composable
private fun ReviewPane(
    uiState: ReasonDetailUiState,
    modifier: Modifier = Modifier,
    onReveal: () -> Unit,
    onReview: (String) -> Unit,
    onNext: () -> Unit,
    onQuestionClick: (Long) -> Unit,
) {
    val item = uiState.currentItem ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(NotionSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NotionSpacing.md),
    ) {
        Text(
            "第 ${uiState.currentIndex + 1} / ${uiState.questions.size} 题",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        QuestionImage(path = item.imagePath)

        TextField2(text = item.ocrText, fallback = "无识别文本")

        if (!uiState.revealed) {
            HintCard(text = "先想一想：我为什么错？")
            Button(
                onClick = onReveal,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.full,
            ) {
                Text("显示错因", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(NotionSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(NotionSpacing.xs),
                ) {
                    Text(
                        uiState.reason?.name.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    item.detail?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    item.note?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            "备注：$it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NotionSpacing.xs),
            ) {
                FeedbackButton("记住了", Modifier.weight(1f)) { onReview(ReviewResult.REMEMBERED) }
                FeedbackButton("又错", Modifier.weight(1f), emphasise = true) { onReview(ReviewResult.WRONG) }
                FeedbackButton("模糊", Modifier.weight(1f)) { onReview(ReviewResult.FUZZY) }
            }

            OutlinedButton(
                onClick = { onQuestionClick(item.questionId) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("查看题目详情")
            }
        }

        if (uiState.isLast && uiState.revealed) {
            Text(
                "这一组复习完成了",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            OutlinedButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text("下一题")
            }
        }
    }
}

@Composable
private fun FeedbackButton(label: String, modifier: Modifier = Modifier, emphasise: Boolean = false, onClick: () -> Unit) {
    if (emphasise) {
        Button(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.full) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.full) { Text(label) }
    }
}

@Composable
private fun HintCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(NotionSpacing.lg),
        )
    }
}

@Composable
private fun QuestionImage(path: String) {
    val file = remember(path) { File(path) }
    if (!file.exists()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "图片已丢失",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(NotionSpacing.lg),
            )
        }
    } else {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
        )
    }
}

@Composable
private fun TextField2(text: String?, fallback: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text?.takeIf { it.isNotBlank() } ?: fallback,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(NotionSpacing.md),
        )
    }
}