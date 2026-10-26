package com.expensetracker.app.di

import android.content.Context
import com.expensetracker.app.data.local.ExpenseTrackerDatabase
import com.expensetracker.app.data.local.dao.BudgetDao
import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.dao.DebtDao
import com.expensetracker.app.data.local.dao.RecurringDao
import com.expensetracker.app.data.local.dao.SavingsDao
import com.expensetracker.app.data.local.dao.TransactionDao
import com.expensetracker.app.data.local.dao.WalletDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ExpenseTrackerDatabase {
        return ExpenseTrackerDatabase.buildDatabase(context)
    }

    @Provides
    fun provideTransactionDao(db: ExpenseTrackerDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: ExpenseTrackerDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideWalletDao(db: ExpenseTrackerDatabase): WalletDao = db.walletDao()

    @Provides
    fun provideBudgetDao(db: ExpenseTrackerDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideSavingsDao(db: ExpenseTrackerDatabase): SavingsDao = db.savingsDao()

    @Provides
    fun provideDebtDao(db: ExpenseTrackerDatabase): DebtDao = db.debtDao()

    @Provides
    fun provideRecurringDao(db: ExpenseTrackerDatabase): RecurringDao = db.recurringDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
