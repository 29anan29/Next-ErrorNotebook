package com.errorbook.app.data.repository

import com.errorbook.app.data.local.dao.SubjectDao
import com.errorbook.app.data.local.entity.SubjectEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SubjectRepository @Inject constructor(
    private val subjectDao: SubjectDao,
) {
    fun getAll(): Flow<List<SubjectEntity>> = subjectDao.getAll()

    suspend fun getById(id: Long): SubjectEntity? = subjectDao.getById(id)

    suspend fun add(name: String): Long = subjectDao.insert(SubjectEntity(name = name.trim()))

    suspend fun rename(subject: SubjectEntity, newName: String) = subjectDao.update(subject.copy(name = newName.trim()))

    suspend fun delete(subject: SubjectEntity) = subjectDao.delete(subject)

    suspend fun deleteAll() = subjectDao.deleteAll()
}
