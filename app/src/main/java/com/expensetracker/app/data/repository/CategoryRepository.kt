package com.expensetracker.app.data.repository

import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.data.local.ExpenseTrackerDatabase
import com.expensetracker.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val database: ExpenseTrackerDatabase
) {
    private val categoryDao = database.categoryDao()

    fun getAllActiveCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllActive()

    fun getActiveCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>> =
        categoryDao.getActiveByType(type)

    suspend fun getCategoryById(id: Long): CategoryEntity? = withContext(Dispatchers.IO) {
        categoryDao.getById(id)
    }

    suspend fun addCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        categoryDao.insert(category)
    }

    suspend fun updateCategory(category: CategoryEntity): Unit = withContext(Dispatchers.IO) {
        categoryDao.update(category)
    }

    suspend fun archiveCategory(id: Long): Unit = withContext(Dispatchers.IO) {
        categoryDao.archive(id)
    }
}
