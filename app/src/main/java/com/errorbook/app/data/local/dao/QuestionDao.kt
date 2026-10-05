package com.errorbook.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.errorbook.app.data.local.entity.QuestionEntity
import com.errorbook.app.data.model.QuestionReasonDisplay
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: Long): QuestionEntity?

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(question: QuestionEntity): Long

    @Update
    suspend fun update(question: QuestionEntity)

    @Delete
    suspend fun delete(question: QuestionEntity)

    @Query("DELETE FROM questions")
    suspend fun deleteAll()

    /** 某道题的全部错因（含具体说明），供题目详情页使用。 */
    @Query(
        """
        SELECT qr.id AS questionReasonId, qr.reasonId AS reasonId, r.name AS reasonName,
               r.category AS category, qr.detail AS detail
        FROM question_reasons qr
        JOIN reasons r ON r.id = qr.reasonId
        WHERE qr.questionId = :questionId
        ORDER BY qr.createdAt ASC
        """,
    )
    fun getReasonsForQuestion(questionId: Long): Flow<List<QuestionReasonDisplay>>

    /** 事务式删除题目（级联删除关联与复习记录）。 */
    @Transaction
    suspend fun deleteQuestionWithLinks(question: QuestionEntity) = delete(question)
}