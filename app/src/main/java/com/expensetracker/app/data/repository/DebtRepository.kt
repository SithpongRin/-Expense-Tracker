package com.expensetracker.app.data.repository

import androidx.room.withTransaction
import com.expensetracker.app.core.model.DebtType
import com.expensetracker.app.data.local.ExpenseTrackerDatabase
import com.expensetracker.app.data.local.entity.DebtEntity
import com.expensetracker.app.data.local.entity.DebtRepaymentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtRepository @Inject constructor(
    private val database: ExpenseTrackerDatabase
) {
    private val debtDao = database.debtDao()
    private val walletDao = database.walletDao()

    fun getAllDebts(): Flow<List<DebtEntity>> = debtDao.getAllDebts()

    fun getDebtsByType(type: DebtType): Flow<List<DebtEntity>> = debtDao.getDebtsByType(type)

    fun getRepaymentsForDebt(debtId: Long): Flow<List<DebtRepaymentEntity>> =
        debtDao.getRepaymentsForDebt(debtId)

    suspend fun getDebtById(id: Long): DebtEntity? = withContext(Dispatchers.IO) {
        debtDao.getDebtById(id)
    }

    suspend fun addDebt(debt: DebtEntity): Long = withContext(Dispatchers.IO) {
        debtDao.insertDebt(debt)
    }

    suspend fun updateDebt(debt: DebtEntity): Unit = withContext(Dispatchers.IO) {
        debtDao.updateDebt(debt)
    }

    suspend fun deleteDebt(debt: DebtEntity): Unit = withContext(Dispatchers.IO) {
        debtDao.deleteDebt(debt)
    }

    /**
     * Atomically adds a repayment, decrements remaining debt balance, and adjusts wallet balance.
     */
    suspend fun addRepayment(repayment: DebtRepaymentEntity): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val debt = debtDao.getDebtById(repayment.debtId)
                ?: throw IllegalArgumentException("Debt record not found")

            val newRemaining = debt.remainingAmount.subtract(repayment.amount).max(BigDecimal.ZERO)
            val isSettled = newRemaining.compareTo(BigDecimal.ZERO) <= 0
            debtDao.updateRemaining(debt.id, newRemaining, isSettled)

            if (repayment.walletId != null) {
                val wallet = walletDao.getById(repayment.walletId)
                if (wallet != null) {
                    val newWalletBalance = when (debt.type) {
                        DebtType.I_OWE -> wallet.currentBalance.subtract(repayment.amount)
                        DebtType.THEY_OWE -> wallet.currentBalance.add(repayment.amount)
                    }
                    walletDao.updateBalance(wallet.id, newWalletBalance)
                }
            }

            debtDao.insertRepayment(repayment)
        }
    }
}
