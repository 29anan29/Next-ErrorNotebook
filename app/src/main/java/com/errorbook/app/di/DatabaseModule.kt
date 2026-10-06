package com.errorbook.app.di

import android.content.Context
import androidx.room.Room
import com.errorbook.app.data.local.AppDatabase
import com.errorbook.app.data.local.PresetReasons
import com.errorbook.app.data.local.dao.QuestionDao
import com.errorbook.app.data.local.dao.ReasonDao
import com.errorbook.app.data.local.dao.ReviewLogDao
import com.errorbook.app.data.local.dao.SubjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "errorbook.db")
        .addCallback(object : androidx.room.RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 首次建库时批量预置 14 条错因（PRD §11 附录）。
                val now = System.currentTimeMillis()
                PresetReasons.PRESETS.forEach { preset ->
                    db.execSQL(
                        "INSERT INTO reasons (name, category, color, isArchived, createdAt) VALUES (?, ?, ?, 0, ?)",
                        arrayOf<Any>(preset.name, preset.category, preset.color, now),
                    )
                }
            }
        })
        .build()

    @Provides
    fun provideSubjectDao(db: AppDatabase): SubjectDao = db.subjectDao()

    @Provides
    fun provideQuestionDao(db: AppDatabase): QuestionDao = db.questionDao()

    @Provides
    fun provideReasonDao(db: AppDatabase): ReasonDao = db.reasonDao()

    @Provides
    fun provideReviewLogDao(db: AppDatabase): ReviewLogDao = db.reviewLogDao()
}
