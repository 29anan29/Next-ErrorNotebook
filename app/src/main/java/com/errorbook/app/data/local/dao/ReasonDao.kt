package com.errorbook.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.errorbook.app.data.local.entity.QuestionReasonEntity
import com.errorbook.app.data.local.entity.ReasonEntity
import com.errorbook.app.data.model.ReasonQuestionItem
import com.errorbook.app.data.model.ReasonWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface ReasonDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reason: ReasonEntity): Long

    @Update
    suspend fun update(reason: ReasonEntity)

    @Delete
    suspend fun delete(reason: ReasonEntity)

    @Query("SELECT * FROM reasons WHERE isArchived = 0 ORDER BY name")
    fun getActive(): Flow<List<ReasonEntity>>

    @Query("SELECT * FROM reasons WHERE id = :id")
    suspend fun getById(id: Long): ReasonEntity?

    @Query("SELECT * FROM reasons WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ReasonEntity?

    @Query("DELETE FROM reasons")
    suspend fun deleteAll()

    /**
     * 排行榜主查询：
     * wrongCount    = COUNT(qr.id)，该错因被标记/被「又错」的总次数；
     * questionCount = COUNT(DISTINCT qr.questionId)，关联的不同题目数；
     * lastWrongAt   = 最近一次关联时间，用作同次数时的第二排序键。
     *
     * subjectId / since 均为可空参数，NULL 表示不过滤——科目与时间可任意组合。
     */
    @Query(
        """
        SELECT r.id AS id, r.name AS name, r.category AS category, r.color AS color,
               COUNT(qr.id) AS wrongCount,
               COUNT(DISTINCT qr.questionId) AS questionCount,
               MAX(COALESCE(qr.createdAt, 0)) AS lastWrongAt
        FROM reasons r
        LEFT JOIN question_reasons qr ON qr.reasonId = r.id
        LEFT JOIN questions q ON q.id = qr.questionId
        WHERE r.isArchived = 0
          AND (:subjectId IS NULL OR q.subjectId = :subjectId)
          AND (:since IS NULL OR qr.createdAt >= :since)
        GROUP BY r.id
        HAVING COUNT(qr.id) > 0
        ORDER BY wrongCount DESC, lastWrongAt DESC
        """,
    )
    fun getReasonRanking(subjectId: Long?, since: Long?): Flow<List<ReasonWithCount>>

    /** 单个错因的详情统计（错因详情页标题区）。 */
    @Query(
        """
        SELECT r.id AS id, r.name AS name, r.category AS category, r.color AS color,
               COUNT(qr.id) AS wrongCount,
               COUNT(DISTINCT qr.questionId) AS questionCount,
               MAX(COALESCE(qr.createdAt, 0)) AS lastWrongAt
        FROM reasons r
        LEFT JOIN question_reasons qr ON qr.reasonId = r.id
        WHERE r.id = :reasonId
        GROUP BY r.id
        """,
    )
    fun getReasonDetail(reasonId: Long): Flow<ReasonWithCount?>

    /** 最近使用过的错因（录题页快捷选择），同题常用错因排前面。 */
    @Query(
        """
        SELECT r.* FROM reasons r
        JOIN question_reasons qr ON qr.reasonId = r.id
        WHERE r.isArchived = 0
        GROUP BY r.id
        ORDER BY MAX(qr.createdAt) DESC
        LIMIT :limit
        """,
    )
    fun getRecentUsed(limit: Int): Flow<List<ReasonEntity>>

    /** 防重复：搜索名称相似的未归档错因。 */
    @Query("SELECT * FROM reasons WHERE isArchived = 0 AND name LIKE '%' || :keyword || '%'")
    suspend fun searchSimilar(keyword: String): List<ReasonEntity>

    /** 该错因下的全部题目（带本错因对其的具体说明）。 */
    @Query(
        """
        SELECT qr.id AS questionReasonId, q.id AS questionId, q.imagePath AS imagePath,
               q.ocrText AS ocrText, q.note AS note, q.createdAt AS createdAt, qr.detail AS detail
        FROM question_reasons qr
        JOIN questions q ON q.id = qr.questionId
        WHERE qr.reasonId = :reasonId
        ORDER BY q.createdAt DESC
        """,
    )
    fun getQuestionsForReason(reasonId: Long): Flow<List<ReasonQuestionItem>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuestionReason(link: QuestionReasonEntity): Long

    /** 从题目上移除一种错因（题目详情页）。 */
    @Query("DELETE FROM question_reasons WHERE id = :linkId")
    suspend fun deleteQuestionReason(linkId: Long)

    @Transaction
    suspend fun mergeReason(fromId: Long, toId: Long) {
        // 把 fromId 名下的关联整体迁回主错因 toId，保留 detail 与 createdAt。
        reassignLinks(fromId, toId)
        val orphan = getById(fromId)
        if (orphan != null) delete(orphan)
    }

    @Query("UPDATE question_reasons SET reasonId = :toId WHERE reasonId = :fromId")
    suspend fun reassignLinks(fromId: Long, toId: Long)

    @Query("SELECT COUNT(*) FROM question_reasons WHERE reasonId = :reasonId")
    suspend fun linkCount(reasonId: Long): Int
}
