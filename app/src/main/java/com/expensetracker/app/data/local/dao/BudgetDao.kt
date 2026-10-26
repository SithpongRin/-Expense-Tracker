package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey AND categoryId IS NULL LIMIT 1")
    fun getOverallMonthlyBudget(monthKey: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey AND categoryId IS NULL LIMIT 1")
    suspend fun getOverallMonthlyBudgetSync(monthKey: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey AND categoryId = :categoryId LIMIT 1")
    suspend fun getCategoryBudget(monthKey: String, categoryId: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey")
    fun getBudgetsForMonth(monthKey: String): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun delete(id: Long)
}
