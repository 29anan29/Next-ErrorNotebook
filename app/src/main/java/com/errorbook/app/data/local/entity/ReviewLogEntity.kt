package com.errorbook.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 复习反馈结果枚举。 */
object ReviewResult {
    const val REMEMBERED = "remembered"
    const val WRONG = "wrong"
    const val FUZZY = "fuzzy"
}

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = QuestionReasonEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionReasonId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("questionReasonId"), Index("reviewedAt")],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionReasonId: Long,
    val reviewedAt: Long = 0L,
    val result: String = ReviewResult.REMEMBERED,
)
