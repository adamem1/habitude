package com.adam.habituator.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val sortOrder: Int = 0,
    /** Optional weekly target for combined sessions across all habits in this category. */
    val weeklyGoalCount: Int? = null,
)
