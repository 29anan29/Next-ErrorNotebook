package com.errorbook.app.data.model

/** 某道题的一种错因（题目详情页展示用）。 */
data class QuestionReasonDisplay(
    val questionReasonId: Long,
    val reasonId: Long,
    val reasonName: String,
    val category: String?,
    val detail: String?,
)
