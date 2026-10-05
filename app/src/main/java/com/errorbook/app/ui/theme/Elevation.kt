package com.errorbook.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * DESIGN-notion.md 的高度层级，映射到 Compose 的阴影海拔：
 *  - Level 0（扁平）：默认卡片，只加 1dp hairline 边框，无阴影
 *  - Level 1（轻浮）：页面上的浮动按钮/弹出层，多层近透明微阴影
 *  - Level 2（重浮）：模态、底部表单，更深的层叠阴影
 *
 * DESIGN 里是 5 层 rgba 栈，Compose 用 Modifier.shadow() 的单层近似
 * （默认 spotColor/ambientColor 就是近黑），这里按总偏移取档，
 * 既避免过曝也不引入多层叠加的性能开销。
 */
object NotionElevation {
    val flat: Dp = 0.dp
    val soft: Dp = 2.dp
    val elevated: Dp = 8.dp
}