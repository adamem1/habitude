package com.adam.habituator.data.repository

import com.adam.habituator.data.db.HabitItemDao
import com.adam.habituator.data.db.HabitItemEntity
import kotlinx.coroutines.flow.Flow

class HabitRepository(private val dao: HabitItemDao) {
    fun observeItems(): Flow<List<HabitItemEntity>> = dao.observeAll()

    suspend fun getById(id: Long): HabitItemEntity? = dao.getById(id)

    suspend fun save(item: HabitItemEntity): Long =
        if (item.id == 0L) dao.insert(item) else { dao.update(item); item.id }

    suspend fun delete(item: HabitItemEntity) = dao.delete(item)
}
