package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.data.local.entity.CategoryTotal
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.data.local.entity.TransactionWithDetails
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface TransactionDao {

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllWithDetails(): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC LIMIT :limit")
    fun getRecentWithDetails(limit: Int): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getWithDetailsById(id: Long): TransactionWithDetails?

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
          AND (:type IS NULL OR type = :type)
          AND (:walletId IS NULL OR walletId = :walletId OR toWalletId = :walletId)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:searchQuery IS NULL OR merchant LIKE '%' || :searchQuery || '%' OR notes LIKE '%' || :searchQuery || '%')
        ORDER BY 
          CASE WHEN :sortOrder = 'NEWEST' THEN date END DESC,
          CASE WHEN :sortOrder = 'OLDEST' THEN date END ASC,
          CASE WHEN :sortOrder = 'HIGHEST' THEN CAST(amount AS REAL) END DESC,
          CASE WHEN :sortOrder = 'LOWEST' THEN CAST(amount AS REAL) END ASC,
          id DESC
    """)
    fun getFilteredTransactions(
        startDate: Long?,
        endDate: Long?,
        type: TransactionType?,
        walletId: Long?,
        categoryId: Long?,
        searchQuery: String?,
        sortOrder: String = "NEWEST"
    ): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT * FROM transactions 
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY date ASC
    """)
    suspend fun getByDateRange(startDate: Long, endDate: Long): List<TransactionEntity>

    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY date ASC
    """)
    fun getWithDetailsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT COALESCE(SUM(CAST(amount AS REAL)), 0.0) 
        FROM transactions 
        WHERE type = :type AND date >= :startDate AND date <= :endDate
    """)
    fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Flow<Double>

    @Query("""
        SELECT c.id AS categoryId, c.name AS categoryName, 
               SUM(CAST(t.amount AS REAL)) AS totalAmount, 
               COUNT(t.id) AS count
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'EXPENSE' AND t.date >= :startDate AND t.date <= :endDate
        GROUP BY t.categoryId
        ORDER BY totalAmount DESC
    """)
    fun getTopExpenseCategories(startDate: Long, endDate: Long): Flow<List<CategoryTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}
