package com.expensetracker.app.data.repository

import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.core.util.DateTimeUtils
import com.expensetracker.app.data.local.ExpenseTrackerDatabase
import com.expensetracker.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

data class BudgetProgress(
    val budget: BudgetEntity,
    val spentAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val percentageUsed: Float,
    val isWarningThresholdReached: Boolean,
    val isExceeded: Boolean
)

@Singleton
class BudgetRepository @Inject constructor(
    private val database: ExpenseTrackerDatabase
) {
    private val budgetDao = database.budgetDao()
    private val transactionDao = database.transactionDao()

    fun getOverallMonthlyBudget(monthKey: String): Flow<BudgetEntity?> =
        budgetDao.getOverallMonthlyBudget(monthKey)

    fun getBudgetsForMonth(monthKey: String): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(monthKey)

    suspend fun setBudget(budget: BudgetEntity): Long = withContext(Dispatchers.IO) {
        budgetDao.insertOrUpdate(budget)
    }

    suspend fun deleteBudget(id: Long) = withContext(Dispatchers.IO) {
        budgetDao.delete(id)
    }

    suspend fun calculateBudgetProgress(monthKey: String): BudgetProgress? = withContext(Dispatchers.IO) {
        val budget = budgetDao.getOverallMonthlyBudgetSync(monthKey) ?: return@withContext null
        val yearMonth = YearMonth.parse(monthKey)
        val startMillis = DateTimeUtils.getStartOfMonth(yearMonth)
        val endMillis = DateTimeUtils.getEndOfMonth(yearMonth)

        val spentDouble = transactionDao.getTotalByTypeAndDateRange(
            TransactionType.EXPENSE,
            startMillis,
            endMillis
        ).first()

        val spent = BigDecimal(spentDouble).setScale(2, RoundingMode.HALF_EVEN)
        val remaining = budget.amount.subtract(spent)
        val percentage = if (budget.amount > BigDecimal.ZERO) {
            (spent.toDouble() / budget.amount.toDouble()).toFloat() * 100f
        } else {
            0f
        }

        BudgetProgress(
            budget = budget,
            spentAmount = spent,
            remainingAmount = remaining,
            percentageUsed = percentage,
            isWarningThresholdReached = percentage >= budget.warningThresholdPercent,
            isExceeded = spent > budget.amount
        )
    }
}
