package com.errorbook.app.domain.usecase

import com.errorbook.app.data.local.entity.ReviewLogEntity
import com.errorbook.app.data.local.entity.ReviewResult
import com.errorbook.app.data.local.AppDatabase
import com.errorbook.app.data.local.entity.QuestionReasonEntity
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 复习反馈：
 *  - remembered / fuzzy：只写 review_logs；
 *  - wrong（「又错」）：除写 review_logs 外，同一 (questionId, reasonId)
 *    再插入一条新的 question_reasons 关联行——错因计数 = COUNT(qr.id)，
 *    因此 +1，而 DISTINCT 题目数不变；
 *  两组写入放在一个事务里，失败要么都成功、要么都回滚。
 */
class RecordReviewUseCase @Inject constructor(
    private val db: AppDatabase,
) {
    suspend operator fun invoke(
        questionReasonId: Long,
        questionId: Long,
        reasonId: Long,
        result: String,
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            db.reviewLogDao().insert(
                ReviewLogEntity(questionReasonId = questionReasonId, reviewedAt = now, result = result),
            )
            if (result == ReviewResult.WRONG) {
                db.reasonDao().insertQuestionReason(
                    QuestionReasonEntity(
                        questionId = questionId,
                        reasonId = reasonId,
                        detail = null,
                        createdAt = now,
                    ),
                )
            }
        }
    }
}