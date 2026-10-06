package com.errorbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.errorbook.app.ui.theme.NotionShape
import com.errorbook.app.ui.theme.NotionSpacing

/**
 * 错因标签胶囊：白底 + 发丝线边框 + 分类色圆点。
 * 圆点取自装饰性贴纸色板——它只作类别识别，绝不用于 CTA 或结构填充。
 */
@Composable
fun ReasonChip(name: String, modifier: Modifier = Modifier, selected: Boolean = false, categoryColor: Color? = null, trailing: String? = null, onClick: (() -> Unit)? = null) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .clip(NotionShape.full)
            .background(bg)
            .border(1.dp, borderColor, NotionShape.full)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = NotionSpacing.sm, vertical = NotionSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categoryColor?.let {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(it)
                    .padding(3.dp),
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = NotionSpacing.xs),
            )
        }
    }
}
