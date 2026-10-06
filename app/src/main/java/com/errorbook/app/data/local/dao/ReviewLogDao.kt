package com.errorbook.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.errorbook.app.data.local.entity.ReviewLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewLogDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(log: ReviewLogEntity): Long

    @Query("SELECT * FROM review_logs WHERE questionReasonId = :questionReasonId ORDER BY reviewedAt DESC")
    fun getForQuestionReason(questionReasonId: Long): Flow<List<ReviewLogEntity>>

    @Query("DELETE FROM review_logs")
    suspend fun deleteAll()
}
