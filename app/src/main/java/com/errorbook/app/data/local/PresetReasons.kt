package com.errorbook.app.data.local

/**
 * 预置错因（PRD §11 附录）。首次建库时通过 RoomDatabase.Callback 的
 * onCreate 用 execSQL 批量插入；颜色与 ui/theme/Color.kt 的
 * ReasonCategory 枚举保持一一对应，保证各分类圆点全应用同色。
 */
object PresetReasons {
    private const val COLOR_KNOWLEDGE = "#DD5B00" // StickerOrange
    private const val COLOR_HABIT = "#2A9D99" // StickerTeal
    private const val COLOR_STRATEGY = "#D6B6F6" // StickerPurple
    private const val COLOR_PSYCH = "#FF64C8" // StickerPink

    data class Preset(val name: String, val category: String, val color: String)

    val PRESETS: List<Preset> = listOf(
        Preset("概念不清", "知识性", COLOR_KNOWLEDGE),
        Preset("公式记错", "知识性", COLOR_KNOWLEDGE),
        Preset("定理用错", "知识性", COLOR_KNOWLEDGE),
        Preset("知识点遗漏", "知识性", COLOR_KNOWLEDGE),
        Preset("审题失误", "习惯性", COLOR_HABIT),
        Preset("计算错误", "习惯性", COLOR_HABIT),
        Preset("抄写错误", "习惯性", COLOR_HABIT),
        Preset("单位漏看", "习惯性", COLOR_HABIT),
        Preset("思路断", "策略性", COLOR_STRATEGY),
        Preset("方法选错", "策略性", COLOR_STRATEGY),
        Preset("时间分配不当", "策略性", COLOR_STRATEGY),
        Preset("时间不够", "心理时间", COLOR_PSYCH),
        Preset("紧张看错", "心理时间", COLOR_PSYCH),
        Preset("粗心", "心理时间", COLOR_PSYCH),
    )
}
