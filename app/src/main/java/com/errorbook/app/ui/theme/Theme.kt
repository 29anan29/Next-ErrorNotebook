package com.errorbook.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Notion 风格的明亮色彩方案：暖白画布 + 白卡片 + 近黑正文 + 单一结构性蓝。
 * 深色模式列在 PRD 后续版本，V0 不做，保持单一明亮方案。
 */
private val LightColorScheme = lightColorScheme(
    primary = NotionBlue,
    onPrimary = Surface,
    primaryContainer = Color(0xFFE4EFFA),
    onPrimaryContainer = NotionBlue,
    secondary = NotionIndigo,
    onSecondary = Surface,
    tertiary = StickerOrange,
    onTertiary = Surface,
    background = CanvasSoft,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1F0EF),
    onSurfaceVariant = InkSecondary,
    surfaceContainerHighest = CanvasSoft,
    outline = Hairline,
    outlineVariant = Color(0xFFD9D7D5),
    error = StickerOrange,
    onError = Surface,
)

private val NotionTypography = Typography(
    displayLarge = DisplayLarge,
    displayMedium = DisplayMedium,
    headlineLarge = HeadlineLarge,
    headlineMedium = HeadlineMedium,
    titleLarge = TitleLarge,
    titleMedium = TitleMedium,
    bodyLarge = BodyLarge,
    bodyMedium = BodyMedium,
    bodySmall = BodySmall,
    labelLarge = LabelLarge,
    labelSmall = LabelSmall,
)

private val NotionShapes = Shapes(
    extraSmall = NotionShape.extraSmall,
    small = NotionShape.small,
    medium = NotionShape.medium,
    large = NotionShape.large,
    extraLarge = NotionShape.extraLarge,
)

@Composable
fun ErrorBookTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = NotionTypography,
        shapes = NotionShapes,
        content = content,
    )
}