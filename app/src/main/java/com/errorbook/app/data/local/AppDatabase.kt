package com.errorbook.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.errorbook.app.data.local.dao.QuestionDao
import com.errorbook.app.data.local.dao.ReasonDao
import com.errorbook.app.data.local.dao.ReviewLogDao
import com.errorbook.app.data.local.dao.SubjectDao
import com.errorbook.app.data.local.entity.QuestionEntity
import com.errorbook.app.data.local.entity.QuestionReasonEntity
import com.errorbook.app.data.local.entity.ReasonEntity
import com.errorbook.app.data.local.entity.ReviewLogEntity
import com.errorbook.app.data.local.entity.SubjectEntity

@Database(
    entities = [
        SubjectEntity::class,
        QuestionEntity::class,
        ReasonEntity::class,
        QuestionReasonEntity::class,
        ReviewLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun questionDao(): QuestionDao
    abstract fun reasonDao(): ReasonDao
    abstract fun reviewLogDao(): ReviewLogDao
}
