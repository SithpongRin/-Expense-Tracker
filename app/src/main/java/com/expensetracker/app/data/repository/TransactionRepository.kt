package com.expensetracker.app.data.repository

import androidx.room.withTransaction
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.data.local.ExpenseTrackerDatabase
import com.expensetracker.app.data.local.entity.CategoryTotal
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.data.local.entity.TransactionWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val database: ExpenseTrackerDatabase
) {
    private val transactionDao = database.transactionDao()
    private val walletDao = database.walletDao()

    fun getAllWithDetailsFlow(): Flow<List<TransactionWithDetails>> =
        transactionDao.getAllWithDetails()

    fun getRecentWithDetailsFlow(limit: Int = 10): Flow<List<TransactionWithDetails>> =
        transactionDao.getRecentWithDetails(limit)

    fun getFilteredTransactions(
        startDate: Long?,
        endDate: Long?,
        type: TransactionType?,
        walletId: Long?,
        categoryId: Long?,
        searchQuery: String?,
        sortOrder: String = "NEWEST"
    ): Flow<List<TransactionWithDetails>> =
        transactionDao.getFilteredTransactions(
            startDate, endDate, type, walletId, categoryId,
            if (searchQuery.isNullOrBlank()) null else searchQuery.trim(),
            sortOrder
        )

    fun getWithDetailsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionWithDetails>> =
        transactionDao.getWithDetailsByDateRange(startDate, endDate)

    fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> =
        transactionDao.getTotalByTypeAndDateRange(type, startDate, endDate)

    fun getTopExpenseCategories(startDate: Long, endDate: Long): Flow<List<CategoryTotal>> =
        transactionDao.getTopExpenseCategories(startDate, endDate)

    suspend fun getTransactionById(id: Long): TransactionWithDetails? = withContext(Dispatchers.IO) {
        transactionDao.getWithDetailsById(id)
    }

    /**
     * Atomically inserts a transaction and adjusts the associated wallet balance(s).
     */
    suspend fun addTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val id = transactionDao.insert(transaction)

            when (transaction.type) {
                TransactionType.EXPENSE -> {
                    val wallet = walletDao.getById(transaction.walletId)
                    if (wallet != null) {
                        walletDao.updateBalance(wallet.id, wallet.currentBalance.subtract(transaction.amount))
                    }
                }
                TransactionType.INCOME -> {
                    val wallet = walletDao.getById(transaction.walletId)
                    if (wallet != null) {
                        walletDao.updateBalance(wallet.id, wallet.currentBalance.add(transaction.amount))
                    }
                }
                TransactionType.TRANSFER -> {
                    val sourceWallet = walletDao.getById(transaction.walletId)
                    if (sourceWallet != null) {
                        walletDao.updateBalance(sourceWallet.id, sourceWallet.currentBalance.subtract(transaction.amount))
                    }
                    if (transaction.toWalletId != null) {
                        val destWallet = walletDao.getById(transaction.toWalletId)
                        if (destWallet != null) {
                            walletDao.updateBalance(destWallet.id, destWallet.currentBalance.add(transaction.amount))
                        }
                    }
                }
            }
            id
        }
    }

    /**
     * Atomically updates a transaction and recalculates the balance diff.
     */
    suspend fun updateTransaction(newTransaction: TransactionEntity): Unit = withContext(Dispatchers.IO) {
        database.withTransaction {
            val oldTransaction = transactionDao.getById(newTransaction.id) ?: return@withTransaction

            // Revert old transaction effect
            when (oldTransaction.type) {
                TransactionType.EXPENSE -> {
                    val wallet = walletDao.getById(oldTransaction.walletId)
                    if (wallet != null) {
                        walletDao.updateBalance(wallet.id, wallet.currentBalance.add(oldTransaction.amount))
                    }
                }
                TransactionType.INCOME -> {
                    val wallet = walletDao.getById(oldTransaction.walletId)
                    if (wallet != null) {
                        walletDao.updateBalance(wallet.id, wallet.currentBalance.subtract(oldTransaction.amount))
                    }
                }
                TransactionType.TRANSFER -> {
                    val sourceWallet = walletDao.getById(oldTransaction.walletId)
                    if (sourceWallet != null) {
                        walletDao.updateBalance(sourceWallet.id, sourceWallet.currentBalance.add(oldTransaction.amount))
                    }
                    if (oldTransaction.toWalletId != null) {
                        val destWallet = walletDao.getById(oldTransaction.toWalletId)
                        if (destWallet != null) {
                            walletDao.updateBalance(destWallet.id, destWallet.currentBalance.subtract(oldTransaction.amount))
                        }
                    }
                }
            }

            // Apply new transaction effect
            when (newTransaction.type) {
                TransactionType.EXPENSE -> {
                    val wallet = walletDao.getById(newTransaction.walletId)
                    if (wallet != null) {
                        walletDao.updateBalance(wallet.id, wallet.currentBalance.subtract(newTransaction.amount))
                    }
                }
                TransactionType.INCOME -> {
                    val wallet = walletDao.getById(newTransaction.walletId)
                    if (wallet != null) {
                        walletDao.updateBalance(wallet.id, wallet.currentBalance.add(newTransaction.amount))
                    }
                }
                TransactionType.TRANSFER -> {
                    val sourceWallet = walletDao.getById(newTransaction.walletId)
                    if (sourceWallet != null) {
                        walletDao.updateBalance(sourceWallet.id, sourceWallet.currentBalance.subtract(newTransaction.amount))
                    }
                    if (newTransaction.toWalletId != null) {
                        val destWallet = walletDao.getById(newTransaction.toWalletId)
                        if (destWallet != null) {
                            walletDao.updateBalance(destWallet.id, destWallet.currentBalance.add(newTransaction.amount))
                        }
                    }
                }
            }

            transactionDao.update(newTransaction)
        }
    }

    /**
     * Atomically deletes a transaction and restores the previous wallet balance.
     */
    suspend fun deleteTransaction(id: Long): Unit = withContext(Dispatchers.IO) {
        database.withTransaction {
            val oldTransaction = transactionDao.getById(id) ?: return@withTransaction

            // Revert effect on wallet(s)
            when (oldTransaction.type) {
                TransactionType.EXPENSE -> {
                    val wallet = walletDao.getById(oldTransaction.walletId)
                    if (wallet != null) {
                        walletDao.updateBalance(wallet.id, wallet.currentBalance.add(oldTransaction.amount))
                    }
                }
                TransactionType.INCOME -> {
                    val wallet = walletDao.getById(oldTransaction.walletId)
                    if (wallet != null) {
                        walletDao.updateBalance(wallet.id, wallet.currentBalance.subtract(oldTransaction.amount))
                    }
                }
                TransactionType.TRANSFER -> {
                    val sourceWallet = walletDao.getById(oldTransaction.walletId)
                    if (sourceWallet != null) {
                        walletDao.updateBalance(sourceWallet.id, sourceWallet.currentBalance.add(oldTransaction.amount))
                    }
                    if (oldTransaction.toWalletId != null) {
                        val destWallet = walletDao.getById(oldTransaction.toWalletId)
                        if (destWallet != null) {
                            walletDao.updateBalance(destWallet.id, destWallet.currentBalance.subtract(oldTransaction.amount))
                        }
                    }
                }
            }

            transactionDao.deleteById(id)
        }
    }
}
