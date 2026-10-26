package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.app.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface WalletDao {

    @Query("SELECT * FROM wallets WHERE isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun getAllActive(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getById(id: Long): WalletEntity?

    @Query("SELECT * FROM wallets WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<WalletEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallet: WalletEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wallets: List<WalletEntity>)

    @Update
    suspend fun update(wallet: WalletEntity)

    @Query("UPDATE wallets SET currentBalance = :newBalance WHERE id = :walletId")
    suspend fun updateBalance(walletId: Long, newBalance: BigDecimal)

    @Query("UPDATE wallets SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("SELECT COUNT(*) FROM wallets")
    suspend fun count(): Int
}
