package com.errorbook.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * 类型层级取自 DESIGN-notion.md，但按移动端实际自适应缩放：
 * 桌面版 64px Display 在手机上没有意义，这里把 Display 降到 32sp 档，
 * 其余角色按 ±2sp 的移动步长保留权重与负字距对比。
 *
 * 字体：NotionInter 是为 Notion 调校的 Inter，无法直接分发；
 * 这里用系统 SansSerif（Android 上即 Roboto），保留原字阶的负 tracking。
 */
private val MobileFontFamily = FontFamily.SansSerif

// 负字距按 DESIGN 中的比例保留：display 级约 −3.3%，heading 约 −2.4%，title 约 −0.6%
val DisplayLarge = TextStyle(
    fontFamily = MobileFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    letterSpacing = (-2.125f / 64f).em,
)

val DisplayMedium = TextStyle(
    fontFamily = MobileFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 33.sp,
    letterSpacing = (-1.875f / 54f).em,
)

val HeadlineLarge = TextStyle(
    fontFamily = MobileFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = (-1.0f / 40f).em,
)

val HeadlineMedium = TextStyle(
    fontFamily = MobileFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.25f / 22f).em,
)

val TitleLarge = TextStyle(
    fontFamily = MobileFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.125f / 20f).em,
)

val TitleMedium = TextStyle(
    fontFamily = MobileFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 24.sp,
)

val BodyLarge = TextStyle(
    fontFamily = MobileFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
)

val BodyMedium = TextStyle(
    fontFamily = MobileFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 20.sp,
)

val LabelLarge = TextStyle(
    fontFamily = MobileFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 24.sp,
)

val BodySmall = TextStyle(
    fontFamily = MobileFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
)

val LabelSmall = TextStyle(
    fontFamily = MobileFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.01.em,
)
