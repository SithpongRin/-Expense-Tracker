package com.expensetracker.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.app.core.model.DebtType
import com.expensetracker.app.core.model.RecurringFrequency
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.core.model.WalletType
import com.expensetracker.app.data.local.dao.BudgetDao
import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.dao.DebtDao
import com.expensetracker.app.data.local.dao.RecurringDao
import com.expensetracker.app.data.local.dao.SavingsDao
import com.expensetracker.app.data.local.dao.TransactionDao
import com.expensetracker.app.data.local.dao.WalletDao
import com.expensetracker.app.data.local.entity.BudgetEntity
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.DebtEntity
import com.expensetracker.app.data.local.entity.DebtRepaymentEntity
import com.expensetracker.app.data.local.entity.RecurringTransactionEntity
import com.expensetracker.app.data.local.entity.SavingsContributionEntity
import com.expensetracker.app.data.local.entity.SavingsGoalEntity
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.data.local.entity.WalletEntity
import com.expensetracker.app.data.repository.DebtRepository
import com.expensetracker.app.data.repository.RecurringRepository
import com.expensetracker.app.data.repository.SavingsRepository
import com.expensetracker.app.data.repository.TransactionRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {

    private lateinit var database: ExpenseTrackerDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var walletDao: WalletDao
    private lateinit var budgetDao: BudgetDao
    private lateinit var savingsDao: SavingsDao
    private lateinit var debtDao: DebtDao
    private lateinit var recurringDao: RecurringDao

    private lateinit var transactionRepo: TransactionRepository
    private lateinit var savingsRepo: SavingsRepository
    private lateinit var debtRepo: DebtRepository
    private lateinit var recurringRepo: RecurringRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ExpenseTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        transactionDao = database.transactionDao()
        categoryDao = database.categoryDao()
        walletDao = database.walletDao()
        budgetDao = database.budgetDao()
        savingsDao = database.savingsDao()
        debtDao = database.debtDao()
        recurringDao = database.recurringDao()

        transactionRepo = TransactionRepository(database)
        savingsRepo = SavingsRepository(database)
        debtRepo = DebtRepository(database)
        recurringRepo = RecurringRepository(database, transactionRepo)
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveWallet() = runBlocking {
        val wallet = WalletEntity(
            name = "ABA Bank",
            type = WalletType.ABA,
            initialBalance = BigDecimal("500.00"),
            currentBalance = BigDecimal("500.00")
        )
        val id = walletDao.insert(wallet)
        val retrieved = walletDao.getById(id)
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.name).isEqualTo("ABA Bank")
        assertThat(retrieved.currentBalance.compareTo(BigDecimal("500.00"))).isEqualTo(0)
    }

    @Test
    fun atomicExpenseTransactionDeductsWalletBalance() = runBlocking {
        val walletId = walletDao.insert(
            WalletEntity(name = "Cash", initialBalance = BigDecimal("100.00"), currentBalance = BigDecimal("100.00"))
        )
        val catId = categoryDao.insert(
            CategoryEntity(name = "Coffee", type = TransactionType.EXPENSE)
        )

        val transaction = TransactionEntity(
            type = TransactionType.EXPENSE,
            amount = BigDecimal("4.50"),
            walletId = walletId,
            categoryId = catId,
            date = System.currentTimeMillis(),
            merchant = "Starbucks"
        )
        transactionRepo.addTransaction(transaction)

        val updatedWallet = walletDao.getById(walletId)!!
        assertThat(updatedWallet.currentBalance.compareTo(BigDecimal("95.50"))).isEqualTo(0)

        val transactions = transactionRepo.getAllWithDetailsFlow().first()
        assertThat(transactions).hasSize(1)
        assertThat(transactions[0].category?.name).isEqualTo("Coffee")
        assertThat(transactions[0].wallet.name).isEqualTo("Cash")
    }

    @Test
    fun atomicIncomeTransactionIncreasesWalletBalance() = runBlocking {
        val walletId = walletDao.insert(
            WalletEntity(name = "ABA", initialBalance = BigDecimal("200.00"), currentBalance = BigDecimal("200.00"))
        )

        val transaction = TransactionEntity(
            type = TransactionType.INCOME,
            amount = BigDecimal("1500.00"),
            walletId = walletId,
            date = System.currentTimeMillis(),
            merchant = "Monthly Salary"
        )
        transactionRepo.addTransaction(transaction)

        val updatedWallet = walletDao.getById(walletId)!!
        assertThat(updatedWallet.currentBalance.compareTo(BigDecimal("1700.00"))).isEqualTo(0)
    }

    @Test
    fun atomicTransferAdjustsBothWalletsWithoutDistortingIncomeOrExpense() = runBlocking {
        val sourceId = walletDao.insert(
            WalletEntity(name = "ABA Bank", initialBalance = BigDecimal("1000.00"), currentBalance = BigDecimal("1000.00"))
        )
        val destId = walletDao.insert(
            WalletEntity(name = "Cash", initialBalance = BigDecimal("50.00"), currentBalance = BigDecimal("50.00"))
        )

        val transfer = TransactionEntity(
            type = TransactionType.TRANSFER,
            amount = BigDecimal("200.00"),
            walletId = sourceId,
            toWalletId = destId,
            date = System.currentTimeMillis()
        )
        transactionRepo.addTransaction(transfer)

        val sourceAfter = walletDao.getById(sourceId)!!
        val destAfter = walletDao.getById(destId)!!

        assertThat(sourceAfter.currentBalance.compareTo(BigDecimal("800.00"))).isEqualTo(0)
        assertThat(destAfter.currentBalance.compareTo(BigDecimal("250.00"))).isEqualTo(0)

        // Expense & Income should remain 0 for period totals
        val expenseTotal = transactionRepo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, 0, Long.MAX_VALUE).first()
        val incomeTotal = transactionRepo.getTotalByTypeAndDateRange(TransactionType.INCOME, 0, Long.MAX_VALUE).first()

        assertThat(expenseTotal).isEqualTo(0.0)
        assertThat(incomeTotal).isEqualTo(0.0)
    }

    @Test
    fun deletingTransactionRestoresWalletBalance() = runBlocking {
        val walletId = walletDao.insert(
            WalletEntity(name = "Cash", initialBalance = BigDecimal("100.00"), currentBalance = BigDecimal("100.00"))
        )
        val transaction = TransactionEntity(
            type = TransactionType.EXPENSE,
            amount = BigDecimal("30.00"),
            walletId = walletId,
            date = System.currentTimeMillis()
        )
        val tId = transactionRepo.addTransaction(transaction)
        assertThat(walletDao.getById(walletId)!!.currentBalance.compareTo(BigDecimal("70.00"))).isEqualTo(0)

        // Delete
        transactionRepo.deleteTransaction(tId)
        assertThat(walletDao.getById(walletId)!!.currentBalance.compareTo(BigDecimal("100.00"))).isEqualTo(0)
        assertThat(transactionDao.getById(tId)).isNull()
    }

    @Test
    fun savingsContributionUpdatesGoalAndSourceWallet() = runBlocking {
        val walletId = walletDao.insert(
            WalletEntity(name = "Savings Account", initialBalance = BigDecimal("1000.00"), currentBalance = BigDecimal("1000.00"))
        )
        val goalId = savingsRepo.addGoal(
            SavingsGoalEntity(name = "New Laptop", targetAmount = BigDecimal("1500.00"))
        )

        savingsRepo.addContribution(
            SavingsContributionEntity(
                goalId = goalId,
                walletId = walletId,
                amount = BigDecimal("300.00")
            )
        )

        val goalAfter = savingsRepo.getGoalById(goalId)!!
        val walletAfter = walletDao.getById(walletId)!!

        assertThat(goalAfter.currentAmount.compareTo(BigDecimal("300.00"))).isEqualTo(0)
        assertThat(goalAfter.isCompleted).isFalse()
        assertThat(walletAfter.currentBalance.compareTo(BigDecimal("700.00"))).isEqualTo(0)
    }

    @Test
    fun debtRepaymentUpdatesRemainingBalanceAndSettlesWhenPaid() = runBlocking {
        val walletId = walletDao.insert(
            WalletEntity(name = "Cash", initialBalance = BigDecimal("500.00"), currentBalance = BigDecimal("500.00"))
        )
        val debtId = debtRepo.addDebt(
            DebtEntity(
                type = DebtType.I_OWE,
                personName = "John Doe",
                originalAmount = BigDecimal("200.00"),
                remainingAmount = BigDecimal("200.00")
            )
        )

        debtRepo.addRepayment(
            DebtRepaymentEntity(debtId = debtId, walletId = walletId, amount = BigDecimal("200.00"))
        )

        val debtAfter = debtRepo.getDebtById(debtId)!!
        val walletAfter = walletDao.getById(walletId)!!

        assertThat(debtAfter.remainingAmount.compareTo(BigDecimal.ZERO)).isEqualTo(0)
        assertThat(debtAfter.isSettled).isTrue()
        assertThat(walletAfter.currentBalance.compareTo(BigDecimal("300.00"))).isEqualTo(0)
    }
}
