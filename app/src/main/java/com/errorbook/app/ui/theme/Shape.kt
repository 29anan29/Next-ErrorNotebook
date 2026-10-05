package com.errorbook.app.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * DESIGN-notion.md 的圆角体系：
 *   xs(4) 表单字段/标签 · sm(5) 菜单项/行 · md(8) 小卡片/工具按钮
 *   lg(12) 特性卡片/内容块 · xl(16) 大容器/图片容器 · full(9999) 胶囊
 */
object NotionShape {
    val extraSmall: CornerBasedShape = RoundedCornerShape(4.dp)
    val small: CornerBasedShape = RoundedCornerShape(5.dp)
    val medium: CornerBasedShape = RoundedCornerShape(8.dp)
    val large: CornerBasedShape = RoundedCornerShape(12.dp)
    val extraLarge: CornerBasedShape = RoundedCornerShape(16.dp)
    val full: CornerBasedShape = RoundedCornerShape(9999.dp)
}