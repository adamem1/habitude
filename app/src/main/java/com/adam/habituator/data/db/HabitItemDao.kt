package com.adam.habituator.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitItemDao {
    @Query("SELECT * FROM habit_items WHERE archived = 0 ORDER BY name")
    fun observeAll(): Flow<List<HabitItemEntity>>

    /** Includes archived items, unlike [observeAll] — used for backup export. */
    @Query("SELECT * FROM habit_items")
    suspend fun getAll(): List<HabitItemEntity>

    @Query("SELECT * FROM habit_items WHERE id = :id")
    suspend fun getById(id: Long): HabitItemEntity?

    @Insert
    suspend fun insert(item: HabitItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<HabitItemEntity>)

    @Update
    suspend fun update(item: HabitItemEntity)

    @Delete
    suspend fun delete(item: HabitItemEntity)
}
