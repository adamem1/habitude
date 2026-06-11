package com.adam.habituator.data.repository

import com.adam.habituator.data.db.CategoryDao
import com.adam.habituator.data.db.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {
    fun observeCategories(): Flow<List<CategoryEntity>> = dao.observeAll()

    suspend fun save(category: CategoryEntity): Long =
        if (category.id == 0L) dao.insert(category) else { dao.update(category); category.id }

    suspend fun delete(category: CategoryEntity) = dao.delete(category)
}
