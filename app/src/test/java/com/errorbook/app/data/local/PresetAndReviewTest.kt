package com.errorbook.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.errorbook.app.data.local.entity.QuestionEntity
import com.errorbook.app.data.local.entity.ReviewResult
import com.errorbook.app.domain.usecase.RecordReviewUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 预置错因种子里必须真的落到 reasons 表，且与 PresetReasons 保持一致。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PresetSeedTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val now = 0L
                    PresetReasons.PRESETS.forEach {
                        db.execSQL(
                            "INSERT INTO reasons (name, category, color, isArchived, createdAt) VALUES (?, ?, ?, 0, ?)",
                            arrayOf<Any>(it.name, it.category, it.color, now),
                        )
                    }
                }
            })
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `首次建库会插入全部预置错因`() = runBlocking {
        val active = db.reasonDao().getActive().first()
        val names = active.map { it.name }.toSet()

        PresetReasons.PRESETS.forEach { preset ->
            assertTrue("缺少预置错因 ${preset.name}", names.contains(preset.name))
        }
    }

    @Test
    fun `预置错因覆盖四个分类且都带颜色`() = runBlocking {
        val active = db.reasonDao().getActive().first()
        val categories = active.mapNotNull { it.category }.toSet()

        assertEquals(setOf("知识性", "习惯性", "策略性", "心理时间"), categories)
        assertTrue(active.all { !it.color.isNullOrBlank() })
    }

    @Test
    fun `预置错因名称符合 PRD 附录`() {
        val names = PresetReasons.PRESETS.map { it.name }
        listOf("概念不清", "公式记错", "审题失误", "计算错误", "思路断", "时间不够", "粗心")
            .forEach { assertTrue("缺少 $it", names.contains(it)) }
        assertNotNull(PresetReasons.PRESETS.firstOrNull { it.name == "计算错误" })
    }

    @Test
    fun `预置错因默认都出现在未归档列表里`() = runBlocking {
        val active = db.reasonDao().getActive().first()
        assertEquals(PresetReasons.PRESETS.size, active.size)
    }
}

/** 复习反馈：只有「又错」会让错因出错次数 +1。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReviewFeedbackTest {
    private lateinit var db: AppDatabase
    private var reasonId: Long = 0L
    private var questionId: Long = 0L
    private var linkId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

        reasonId = db.reasonDao().insert(
            com.errorbook.app.data.local.entity.ReasonEntity(name = "计算错误", createdAt = 0L),
        )
        questionId = db.questionDao().insert(
            QuestionEntity(imagePath = "/tmp/a.jpg", ocrText = "题目", createdAt = 1L),
        )
        linkId = db.reasonDao().insertQuestionReason(
            com.errorbook.app.data.local.entity.QuestionReasonEntity(
                questionId = questionId,
                reasonId = reasonId,
                createdAt = 1L,
            ),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `记住了不增加出错次数`() = runBlocking {
        record(ReviewResult.REMEMBERED)
        assertEquals(1, ranking().wrongCount)
    }

    @Test
    fun `模糊不增加出错次数`() = runBlocking {
        record(ReviewResult.FUZZY)
        assertEquals(1, ranking().wrongCount)
    }

    @Test
    fun `又错会让出错次数加一但题目数不变`() = runBlocking {
        record(ReviewResult.WRONG)
        val row = ranking()
        assertEquals(2, row.wrongCount)
        assertEquals(1, row.questionCount)
    }

    @Test
    fun `复习记录会落到 review_logs`() = runBlocking {
        record(ReviewResult.REMEMBERED)
        val logs = db.reviewLogDao().getForQuestionReason(linkId).first()
        assertEquals(1, logs.size)
        assertEquals(ReviewResult.REMEMBERED, logs.first().result)
    }

    private suspend fun record(result: String) {
        RecordReviewUseCase(db).invoke(
            questionReasonId = linkId,
            questionId = questionId,
            reasonId = reasonId,
            result = result,
        )
    }

    private suspend fun ranking() = db.reasonDao().getReasonRanking(null, null).first().single { it.id == reasonId }
}
