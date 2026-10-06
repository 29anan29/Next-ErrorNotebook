package com.errorbook.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reasons",
    indices = [Index(value = ["name"], unique = true)],
)
data class ReasonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String? = null,
    val color: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = 0L,
)
