package com.errorbook.app.data.model

/** 某个错因下的一道题（复习模式用），带当前错因对该题的具体说明。 */
data class ReasonQuestionItem(
    val questionReasonId: Long,
    val questionId: Long,
    val imagePath: String,
    val ocrText: String?,
    val note: String?,
    val createdAt: Long,
    val detail: String?,
)