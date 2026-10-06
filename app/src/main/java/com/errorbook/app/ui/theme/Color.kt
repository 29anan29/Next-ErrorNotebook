package com.errorbook.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── 品牌色 ────────────────────────────────────────────────────────────────────
/** 唯一的结构性强调色：主按钮、正文链接、当前态/焦点信号。不做装饰用途。 */
val NotionBlue = Color(0xFF0075DE)
val NotionBlueActive = Color(0xFF005BAB)
val NotionIndigo = Color(0xFF213183)
val OnNotionBlue = Color(0xFFFFFFFF)

// ── 画布与表面 ────────────────────────────────────────────────────────────────
/** 默认页面底色：温暖的暖白，像纸张而不是冷白。 */
val CanvasSoft = Color(0xFFF6F5F4)
val Surface = Color(0xFFFFFFFF)
val Hairline = Color(0xFFE6E6E6)

// ── 文字 ─────────────────────────────────────────────────────────────────────
val Ink = Color(0xFF000000)
val InkSecondary = Color(0xFF31302E)
val InkMuted = Color(0xFF615D59)
val InkFaint = Color(0xFFA39E98)

// ── 装饰性贴纸色板：只用于分类圆点、插画、标签，绝不用于 CTA 或结构填充 ────────────────
val StickerSky = Color(0xFF62AEF0)
val StickerPurple = Color(0xFFD6B6F6)
val StickerPurpleDeep = Color(0xFF391C57)
val StickerPink = Color(0xFFFF64C8)
val StickerOrange = Color(0xFFDD5B00)
val StickerOrangeDeep = Color(0xFF793400)
val StickerTeal = Color(0xFF2A9D99)
val StickerGreen = Color(0xFF1AAE39)
val StickerBrown = Color(0xFF523410)

/** 错因分类的中文名与贴纸色，与 [PresetReasons] 里的 category 取值保持一致。 */
enum class ReasonCategory(val label: String, val color: Color) {
    KNOWLEDGE("知识性", StickerOrange),
    HABIT("习惯性", StickerTeal),
    STRATEGY("策略性", StickerPurple),
    PSYCHOLOGICAL("心理时间", StickerPink),
    UNCATEGORIZED("未分类", StickerSky),
    ;

    companion object {
        fun fromLabel(label: String?): ReasonCategory = entries.firstOrNull { it.label == label } ?: UNCATEGORIZED
    }
}
