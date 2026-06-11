package com.adam.habituator.data.repository

import com.adam.habituator.data.db.LogEntryDao
import com.adam.habituator.data.db.LogEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class LogRepository(private val dao: LogEntryDao) {
    fun observeAll(): Flow<List<LogEntryEntity>> = dao.observeAll()

    fun observeForItem(habitItemId: Long): Flow<List<LogEntryEntity>> = dao.observeForItem(habitItemId)

    suspend fun logSession(habitItemId: Long, quantity: Double? = null, loggedAt: Instant = Instant.now()): Long =
        dao.insert(LogEntryEntity(habitItemId = habitItemId, loggedAt = loggedAt, quantity = quantity))

    suspend fun update(entry: LogEntryEntity) = dao.update(entry)

    suspend fun delete(entry: LogEntryEntity) = dao.delete(entry)
}
