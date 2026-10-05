package com.errorbook.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 题目与错因的多对多关联。同一 (questionId, reasonId) 允许出现多行——
 * 复习时「又错」会再插入一行，排行榜用 COUNT(qr.id) 统计出错次数，
 * 用 COUNT(DISTINCT qr.questionId) 统计关联题目数。
 */
@Entity(
    tableName = "question_reasons",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ReasonEntity::class,
            parentColumns = ["id"],
            childColumns = ["reasonId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("questionId"), Index("reasonId")],
)
data class QuestionReasonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: Long,
    val reasonId: Long,
    val detail: String?,
    val createdAt: Long,
)