package com.expensetracker.app.data.repository

import androidx.room.withTransaction
import com.expensetracker.app.data.local.ExpenseTrackerDatabase
import com.expensetracker.app.data.local.entity.SavingsContributionEntity
import com.expensetracker.app.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingsRepository @Inject constructor(
    private val database: ExpenseTrackerDatabase
) {
    private val savingsDao = database.savingsDao()
    private val walletDao = database.walletDao()

    fun getAllGoals(): Flow<List<SavingsGoalEntity>> = savingsDao.getAllGoals()

    fun getContributionsForGoal(goalId: Long): Flow<List<SavingsContributionEntity>> =
        savingsDao.getContributionsForGoal(goalId)

    suspend fun getGoalById(id: Long): SavingsGoalEntity? = withContext(Dispatchers.IO) {
        savingsDao.getGoalById(id)
    }

    suspend fun addGoal(goal: SavingsGoalEntity): Long = withContext(Dispatchers.IO) {
        savingsDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: SavingsGoalEntity): Unit = withContext(Dispatchers.IO) {
        savingsDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: SavingsGoalEntity): Unit = withContext(Dispatchers.IO) {
        savingsDao.deleteGoal(goal)
    }

    /**
     * Atomically adds a contribution/withdrawal and optionally updates the source wallet balance.
     */
    suspend fun addContribution(contribution: SavingsContributionEntity): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val goal = savingsDao.getGoalById(contribution.goalId)
                ?: throw IllegalArgumentException("Goal not found")

            val newGoalAmount = if (contribution.isWithdrawal) {
                goal.currentAmount.subtract(contribution.amount).max(BigDecimal.ZERO)
            } else {
                goal.currentAmount.add(contribution.amount)
            }

            val isCompleted = newGoalAmount >= goal.targetAmount
            savingsDao.updateGoalProgress(goal.id, newGoalAmount, isCompleted)

            if (contribution.walletId != null) {
                val wallet = walletDao.getById(contribution.walletId)
                if (wallet != null) {
                    val newWalletBalance = if (contribution.isWithdrawal) {
                        wallet.currentBalance.add(contribution.amount)
                    } else {
                        wallet.currentBalance.subtract(contribution.amount)
                    }
                    walletDao.updateBalance(wallet.id, newWalletBalance)
                }
            }

            savingsDao.insertContribution(contribution)
        }
    }
}
