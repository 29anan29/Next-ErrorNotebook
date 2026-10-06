package com.errorbook.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("subjectId"), Index("createdAt")],
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long? = null,
    val imagePath: String,
    val ocrText: String? = null,
    val note: String? = null,
    val source: String? = null,
    val saveDirUri: String? = null,
    val createdAt: Long = 0L,
    val mastered: Boolean = false,
)
