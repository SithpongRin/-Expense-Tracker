package com.expensetracker.app.data.repository

import androidx.room.withTransaction
import com.expensetracker.app.core.model.RecurringFrequency
import com.expensetracker.app.core.util.DateTimeUtils
import com.expensetracker.app.data.local.ExpenseTrackerDatabase
import com.expensetracker.app.data.local.entity.RecurringTransactionEntity
import com.expensetracker.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepository @Inject constructor(
    private val database: ExpenseTrackerDatabase,
    private val transactionRepository: TransactionRepository
) {
    private val recurringDao = database.recurringDao()

    fun getAllFlow(): Flow<List<RecurringTransactionEntity>> = recurringDao.getAllFlow()

    suspend fun addRecurring(recurring: RecurringTransactionEntity): Long = withContext(Dispatchers.IO) {
        recurringDao.insert(recurring)
    }

    suspend fun updateRecurring(recurring: RecurringTransactionEntity): Unit = withContext(Dispatchers.IO) {
        recurringDao.update(recurring)
    }

    suspend fun deleteRecurring(recurring: RecurringTransactionEntity): Unit = withContext(Dispatchers.IO) {
        recurringDao.delete(recurring)
    }

    suspend fun toggleActive(id: Long, isActive: Boolean): Unit = withContext(Dispatchers.IO) {
        recurringDao.toggleActive(id, isActive)
    }

    /**
     * Finds and processes any due recurring transactions idempotently.
     */
    suspend fun processDueRecurring(): Int = withContext(Dispatchers.IO) {
        val now = DateTimeUtils.nowMillis()
        val dueList = recurringDao.getDueRecurring(now)
        var processedCount = 0

        for (item in dueList) {
            database.withTransaction {
                // Double-check condition inside transaction
                val current = recurringDao.getById(item.id) ?: return@withTransaction
                if (!current.isActive || current.nextOccurrenceDate > now) {
                    return@withTransaction
                }

                // 1. Generate transaction
                val transaction = TransactionEntity(
                    type = current.type,
                    amount = current.amount,
                    currency = current.currency,
                    categoryId = current.categoryId,
                    walletId = current.walletId,
                    date = current.nextOccurrenceDate,
                    merchant = current.merchant,
                    notes = "${current.notes} (Recurring: ${current.frequency})",
                    isRecurring = true,
                    recurringId = current.id
                )
                transactionRepository.addTransaction(transaction)

                // 2. Compute next occurrence
                val nextDate = calculateNextOccurrence(current.nextOccurrenceDate, current.frequency)
                val isExpired = current.endDate != null && nextDate > current.endDate

                recurringDao.update(
                    current.copy(
                        lastProcessedDate = now,
                        nextOccurrenceDate = nextDate,
                        isActive = !isExpired
                    )
                )
                processedCount++
            }
        }
        processedCount
    }

    fun calculateNextOccurrence(currentDateMillis: Long, frequency: RecurringFrequency): Long {
        val localDate = DateTimeUtils.toLocalDate(currentDateMillis)
        val nextLocalDate = when (frequency) {
            RecurringFrequency.DAILY -> localDate.plusDays(1)
            RecurringFrequency.WEEKLY -> localDate.plusWeeks(1)
            RecurringFrequency.MONTHLY -> localDate.plusMonths(1)
            RecurringFrequency.YEARLY -> localDate.plusYears(1)
        }
        return DateTimeUtils.toEpochMillis(nextLocalDate)
    }
}
