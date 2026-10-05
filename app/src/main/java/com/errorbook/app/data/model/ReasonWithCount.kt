package com.errorbook.app.data.model

/**
 * 错因排行榜单项。
 *
 * - [wrongCount]：出错次数 = 该错因的 question_reasons 关联行数（「又错」新增一行，计数 +1）。
 * - [questionCount]：关联的不同题目数（COUNT DISTINCT questionId）。
 * - [lastWrongAt]：最近出错时间，用作次数相同时的第二排序键。
 */
data class ReasonWithCount(
    val id: Long,
    val name: String,
    val category: String?,
    val color: String?,
    val wrongCount: Int,
    val questionCount: Int,
    val lastWrongAt: Long,
)