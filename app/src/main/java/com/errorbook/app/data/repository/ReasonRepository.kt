package com.errorbook.app.data.repository

import com.errorbook.app.data.local.dao.ReasonDao
import com.errorbook.app.data.local.entity.ReasonEntity
import com.errorbook.app.data.model.ReasonWithCount
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReasonRepository @Inject constructor(
    private val reasonDao: ReasonDao,
) {
    fun getRanking(subjectId: Long?, since: Long?): Flow<List<ReasonWithCount>> =
        reasonDao.getReasonRanking(subjectId, since)

    fun getActive(): Flow<List<ReasonEntity>> = reasonDao.getActive()

    fun getRecentUsed(limit: Int): Flow<List<ReasonEntity>> = reasonDao.getRecentUsed(limit)

    fun getDetail(reasonId: Long): Flow<ReasonWithCount?> = reasonDao.getReasonDetail(reasonId)

    suspend fun getById(id: Long): ReasonEntity? = reasonDao.getById(id)

    suspend fun getByName(name: String): ReasonEntity? = reasonDao.getByName(name)

    suspend fun searchSimilar(keyword: String): List<ReasonEntity> = reasonDao.searchSimilar(keyword)

    suspend fun add(name: String, category: String?, color: String?): Long =
        reasonDao.insert(
            ReasonEntity(name = name.trim(), category = category, color = color, createdAt = System.currentTimeMillis()),
        )

    suspend fun rename(reason: ReasonEntity, newName: String) =
        reasonDao.update(reason.copy(name = newName.trim()))

    suspend fun setArchived(reason: ReasonEntity, archived: Boolean) =
        reasonDao.update(reason.copy(isArchived = archived))

    suspend fun delete(reason: ReasonEntity) = reasonDao.delete(reason)

    suspend fun merge(fromId: Long, toId: Long) = reasonDao.mergeReason(fromId, toId)

    suspend fun deleteQuestionReason(linkId: Long) = reasonDao.deleteQuestionReason(linkId)

    suspend fun deleteAll() = reasonDao.deleteAll()
}