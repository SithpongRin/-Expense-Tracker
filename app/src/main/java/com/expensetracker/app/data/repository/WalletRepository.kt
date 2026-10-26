package com.expensetracker.app.data.repository

import com.expensetracker.app.data.local.ExpenseTrackerDatabase
import com.expensetracker.app.data.local.entity.WalletEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(
    private val database: ExpenseTrackerDatabase
) {
    private val walletDao = database.walletDao()

    fun getAllActiveWallets(): Flow<List<WalletEntity>> = walletDao.getAllActive()

    fun getWalletByIdFlow(id: Long): Flow<WalletEntity?> = walletDao.getByIdFlow(id)

    suspend fun getWalletById(id: Long): WalletEntity? = withContext(Dispatchers.IO) {
        walletDao.getById(id)
    }

    suspend fun addWallet(wallet: WalletEntity): Long = withContext(Dispatchers.IO) {
        walletDao.insert(wallet)
    }

    suspend fun updateWallet(wallet: WalletEntity): Unit = withContext(Dispatchers.IO) {
        walletDao.update(wallet)
    }

    suspend fun archiveWallet(id: Long): Unit = withContext(Dispatchers.IO) {
        walletDao.archive(id)
    }
}
