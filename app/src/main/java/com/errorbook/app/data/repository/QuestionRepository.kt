package com.errorbook.app.data.repository

import androidx.room.withTransaction
import com.errorbook.app.data.local.AppDatabase
import com.errorbook.app.data.local.dao.QuestionDao
import com.errorbook.app.data.local.dao.ReasonDao
import com.errorbook.app.data.local.entity.QuestionEntity
import com.errorbook.app.data.local.entity.QuestionReasonEntity
import com.errorbook.app.data.model.QuestionReasonDisplay
import com.errorbook.app.data.model.ReasonQuestionItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val db: AppDatabase,
    private val questionDao: QuestionDao,
    private val reasonDao: ReasonDao,
) {
    fun getReasonsForQuestion(questionId: Long): Flow<List<QuestionReasonDisplay>> =
        questionDao.getReasonsForQuestion(questionId)

    fun getQuestionsForReason(reasonId: Long): Flow<List<ReasonQuestionItem>> =
        reasonDao.getQuestionsForReason(reasonId)

    suspend fun getById(id: Long): QuestionEntity? = questionDao.getById(id)

    suspend fun count(): Int = questionDao.count()

    /** 录题原子操作：先建题再逐条关联错因，全部成功（Room 事务保证）。 */
    suspend fun saveQuestionWithReasons(
        question: QuestionEntity,
        reasonLinks: List<Pair<Long, String?>>, // reasonId to detail
    ): Long = db.withTransaction {
        val questionId = questionDao.insert(question)
        val now = System.currentTimeMillis()
        reasonLinks.forEach { (reasonId, detail) ->
            reasonDao.insertQuestionReason(
                QuestionReasonEntity(
                    questionId = questionId,
                    reasonId = reasonId,
                    detail = detail?.takeIf { it.isNotBlank() },
                    createdAt = now,
                ),
            )
        }
        questionId
    }

    suspend fun update(question: QuestionEntity) = questionDao.update(question)

    suspend fun delete(question: QuestionEntity) = questionDao.deleteQuestionWithLinks(question)

    suspend fun deleteAll() = questionDao.deleteAll()

    suspend fun linkCount(reasonId: Long): Int = reasonDao.linkCount(reasonId)
}