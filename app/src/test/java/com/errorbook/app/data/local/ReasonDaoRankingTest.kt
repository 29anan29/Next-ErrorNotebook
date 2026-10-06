package com.errorbook.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.errorbook.app.data.local.entity.QuestionEntity
import com.errorbook.app.data.local.entity.QuestionReasonEntity
import com.errorbook.app.data.local.entity.ReasonEntity
import com.errorbook.app.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 排行榜统计的核心断言。
 *
 * 这里锁死的是我在规划阶段修正的两个逻辑问题：
 *  1. 出错次数必须随「又错」递增（COUNT(qr.id)，而非 DISTINCT）；
 *  2. 出错次数与关联题目数是两个不同指标，不能互相替代。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReasonDaoRankingTest {
    private lateinit var db: AppDatabase

    private var calcReasonId: Long = 0L
    private var readReasonId: Long = 0L
    private var subjectA: Long = 0L
    private var subjectB: Long = 0L

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

        // 预置错因的父行必须真实存在，否则 question_reasons 的外键会失败。
        calcReasonId = db.reasonDao().insert(
            ReasonEntity(name = "计算错误", category = "习惯性", createdAt = 0L),
        )
        readReasonId = db.reasonDao().insert(
            ReasonEntity(name = "审题失误", category = "习惯性", createdAt = 0L),
        )
        subjectA = db.subjectDao().insert(SubjectEntity(name = "数学"))
        subjectB = db.subjectDao().insert(SubjectEntity(name = "物理"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedQuestion(subjectId: Long? = null, createdAt: Long = 1_000L): Long {
        val id = db.questionDao().insert(
            QuestionEntity(
                subjectId = subjectId,
                imagePath = "/tmp/$createdAt.jpg",
                ocrText = "题目 $createdAt",
                createdAt = createdAt,
            ),
        )
        return id
    }

    private suspend fun link(questionId: Long, reasonId: Long, createdAt: Long = 1_000L) {
        db.reasonDao().insertQuestionReason(
            QuestionReasonEntity(
                questionId = questionId,
                reasonId = reasonId,
                detail = null,
                createdAt = createdAt,
            ),
        )
    }

    @Test
    fun `统计同一题多次标记同一错因时 出错次数按关联行累加`() = runTest {
        val q = seedQuestion()
        link(q, calcReasonId)
        link(q, calcReasonId) // 同一题又错一次

        val ranking = db.reasonDao().getReasonRanking(null, null).first()
        val row = ranking.single { it.id == calcReasonId }

        // 关键：错了两次 → 出错次数 2，而不是被 DISTINCT 吃掉变成 1
        assertEquals(2, row.wrongCount)
        // 但题目只有一道
        assertEquals(1, row.questionCount)
    }

    @Test
    fun `出错次数与关联题目数是两个独立指标`() = runTest {
        val q1 = seedQuestion(createdAt = 1L)
        val q2 = seedQuestion(createdAt = 2L)
        link(q1, calcReasonId)
        link(q2, calcReasonId)
        link(q1, calcReasonId) // 第一题重复错

        val row = db.reasonDao().getReasonRanking(null, null).first().single { it.id == calcReasonId }

        assertEquals(3, row.wrongCount)
        assertEquals(2, row.questionCount)
    }

    @Test
    fun `排行榜按出错次数降序`() = runTest {
        val q = seedQuestion()
        repeat(5) { link(q, calcReasonId) }
        repeat(2) { link(q, readReasonId) }

        val ranking = db.reasonDao().getReasonRanking(null, null).first()

        assertEquals(listOf(calcReasonId, readReasonId), ranking.map { it.id })
    }

    @Test
    fun `出错次数相同时按最近出错时间降序`() = runTest {
        val q = seedQuestion()
        link(q, calcReasonId, createdAt = 100L)
        link(q, readReasonId, createdAt = 900L) // read 更新但次数相同

        val ranking = db.reasonDao().getReasonRanking(null, null).first()

        assertEquals(listOf(readReasonId, calcReasonId), ranking.map { it.id })
    }

    @Test
    fun `归档错因不进入排行榜`() = runTest {
        val q = seedQuestion()
        link(q, calcReasonId)
        db.reasonDao().update(db.reasonDao().getById(calcReasonId)!!.copy(isArchived = true))

        val ranking = db.reasonDao().getReasonRanking(null, null).first()

        assertNull(ranking.firstOrNull { it.id == calcReasonId })
    }

    @Test
    fun `按科目筛选时只统计该科目下的题`() = runTest {
        val qa = seedQuestion(subjectId = subjectA)
        val qb = seedQuestion(subjectId = subjectB)
        link(qa, calcReasonId)
        link(qb, calcReasonId)

        val rankingA = db.reasonDao().getReasonRanking(subjectA, null).first()
        val rowA = rankingA.singleOrNull { it.id == calcReasonId }

        assertEquals(1, rowA?.wrongCount)
        assertEquals(1, rowA?.questionCount)
    }

    @Test
    fun `按时间筛选时只统计 since 之后的关联`() = runTest {
        val q = seedQuestion()
        link(q, calcReasonId, createdAt = 100L)
        link(q, calcReasonId, createdAt = 5_000L)

        val ranking = db.reasonDao().getReasonRanking(null, 1_000L).first()
        val row = ranking.single { it.id == calcReasonId }

        assertEquals(1, row.wrongCount)
    }

    @Test
    fun `科目与时间筛选可以组合`() = runTest {
        val qOld = seedQuestion(subjectId = subjectA)
        val qNew = seedQuestion(subjectId = subjectA, createdAt = 2L)
        val qOther = seedQuestion(subjectId = subjectB, createdAt = 2L)
        link(qOld, calcReasonId, createdAt = 100L)
        link(qNew, calcReasonId, createdAt = 5_000L)
        link(qOther, calcReasonId, createdAt = 5_000L)

        val row = db.reasonDao().getReasonRanking(subjectA, 1_000L).first().single()

        // 只有 qNew 同时满足科目与时间
        assertEquals(1, row.wrongCount)
        assertEquals(1, row.questionCount)
    }

    @Test
    fun `从未被标记的错因不进入排行榜`() = runTest {
        db.reasonDao().insert(ReasonEntity(name = "从没犯过", createdAt = 0L))

        val ranking = db.reasonDao().getReasonRanking(null, null).first()

        assertEquals(0, ranking.count { it.name == "从没犯过" })
    }

    @Test
    fun `删除题目会级联清掉错因关联`() = runTest {
        val q = seedQuestion()
        link(q, calcReasonId)
        val question = db.questionDao().getById(q)!!

        db.questionDao().deleteQuestionWithLinks(question)

        val ranking = db.reasonDao().getReasonRanking(null, null).first()
        assertEquals(0, ranking.count { it.id == calcReasonId })
    }

    @Test
    fun `合并错因会把关联整体迁回目标错因`() = runTest {
        val q = seedQuestion()
        link(q, calcReasonId)
        val from = ReasonEntity(name = "粗心", createdAt = 0L)
        val fromId = db.reasonDao().insert(from)
        link(q, fromId)

        db.reasonDao().mergeReason(fromId, calcReasonId)

        val ranking = db.reasonDao().getReasonRanking(null, null).first()
        val row = ranking.single { it.id == calcReasonId }
        assertEquals(2, row.wrongCount)
        assertNull(db.reasonDao().getById(fromId))
    }
}
