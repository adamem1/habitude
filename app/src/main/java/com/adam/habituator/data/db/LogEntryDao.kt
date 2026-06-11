package com.adam.habituator.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {
    @Query("SELECT * FROM log_entries ORDER BY loggedAt DESC")
    fun observeAll(): Flow<List<LogEntryEntity>>

    @Query("SELECT * FROM log_entries WHERE habitItemId = :habitItemId ORDER BY loggedAt DESC")
    fun observeForItem(habitItemId: Long): Flow<List<LogEntryEntity>>

    @Query("SELECT * FROM log_entries")
    suspend fun getAll(): List<LogEntryEntity>

    @Insert
    suspend fun insert(entry: LogEntryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<LogEntryEntity>)

    @Update
    suspend fun update(entry: LogEntryEntity)

    @Delete
    suspend fun delete(entry: LogEntryEntity)
}
