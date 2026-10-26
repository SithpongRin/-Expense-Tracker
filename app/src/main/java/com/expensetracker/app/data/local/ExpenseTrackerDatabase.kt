package com.expensetracker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        WalletEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        SavingsContributionEntity::class,
        DebtEntity::class,
        DebtRepaymentEntity::class,
        RecurringTransactionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ExpenseTrackerDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun walletDao(): WalletDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsDao(): SavingsDao
    abstract fun debtDao(): DebtDao
    abstract fun recurringDao(): RecurringDao

    companion object {
        const val DATABASE_NAME = "expense_tracker.db"

        fun buildDatabase(context: Context): ExpenseTrackerDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ExpenseTrackerDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate default categories and wallets
                        CoroutineScope(Dispatchers.IO).launch {
                            val instance = buildDatabase(context)
                            seedDefaultData(instance)
                        }
                    }
                })
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }

        suspend fun seedDefaultData(db: ExpenseTrackerDatabase) {
            val categoryDao = db.categoryDao()
            val walletDao = db.walletDao()

            if (categoryDao.count() == 0) {
                val defaultCategories = listOf(
                    // Expense categories
                    CategoryEntity(name = "Food & Dining", nameResKey = "cat_food", type = TransactionType.EXPENSE, iconName = "restaurant", colorHex = "#000000", isDefault = true, sortOrder = 1),
                    CategoryEntity(name = "Drinks", nameResKey = "cat_drinks", type = TransactionType.EXPENSE, iconName = "local_bar", colorHex = "#000000", isDefault = true, sortOrder = 2),
                    CategoryEntity(name = "Coffee", nameResKey = "cat_coffee", type = TransactionType.EXPENSE, iconName = "local_cafe", colorHex = "#000000", isDefault = true, sortOrder = 3),
                    CategoryEntity(name = "Transport", nameResKey = "cat_transport", type = TransactionType.EXPENSE, iconName = "directions_car", colorHex = "#000000", isDefault = true, sortOrder = 4),
                    CategoryEntity(name = "Education", nameResKey = "cat_education", type = TransactionType.EXPENSE, iconName = "school", colorHex = "#000000", isDefault = true, sortOrder = 5),
                    CategoryEntity(name = "Housing & Rent", nameResKey = "cat_housing", type = TransactionType.EXPENSE, iconName = "home", colorHex = "#000000", isDefault = true, sortOrder = 6),
                    CategoryEntity(name = "Bills & Utilities", nameResKey = "cat_bills", type = TransactionType.EXPENSE, iconName = "receipt_long", colorHex = "#000000", isDefault = true, sortOrder = 7),
                    CategoryEntity(name = "Healthcare", nameResKey = "cat_health", type = TransactionType.EXPENSE, iconName = "medical_services", colorHex = "#000000", isDefault = true, sortOrder = 8),
                    CategoryEntity(name = "Entertainment", nameResKey = "cat_entertainment", type = TransactionType.EXPENSE, iconName = "movie", colorHex = "#000000", isDefault = true, sortOrder = 9),
                    CategoryEntity(name = "Shopping", nameResKey = "cat_shopping", type = TransactionType.EXPENSE, iconName = "shopping_bag", colorHex = "#000000", isDefault = true, sortOrder = 10),
                    CategoryEntity(name = "Family", nameResKey = "cat_family", type = TransactionType.EXPENSE, iconName = "people", colorHex = "#000000", isDefault = true, sortOrder = 11),
                    CategoryEntity(name = "Other Expense", nameResKey = "cat_other", type = TransactionType.EXPENSE, iconName = "more_horiz", colorHex = "#000000", isDefault = true, sortOrder = 12),

                    // Income categories
                    CategoryEntity(name = "Salary", nameResKey = "cat_salary", type = TransactionType.INCOME, iconName = "account_balance_wallet", colorHex = "#000000", isDefault = true, sortOrder = 13),
                    CategoryEntity(name = "Business", nameResKey = "cat_business", type = TransactionType.INCOME, iconName = "storefront", colorHex = "#000000", isDefault = true, sortOrder = 14),
                    CategoryEntity(name = "Investment", nameResKey = "cat_investment", type = TransactionType.INCOME, iconName = "trending_up", colorHex = "#000000", isDefault = true, sortOrder = 15),
                    CategoryEntity(name = "Gift", nameResKey = "cat_gift", type = TransactionType.INCOME, iconName = "card_giftcard", colorHex = "#000000", isDefault = true, sortOrder = 16)
                )
                categoryDao.insertAll(defaultCategories)
            }

            if (walletDao.count() == 0) {
                val defaultWallets = listOf(
                    WalletEntity(name = "Cash", type = WalletType.CASH, initialBalance = BigDecimal.ZERO, currentBalance = BigDecimal.ZERO, currency = "USD", sortOrder = 1),
                    WalletEntity(name = "ABA Bank", type = WalletType.ABA, initialBalance = BigDecimal.ZERO, currentBalance = BigDecimal.ZERO, currency = "USD", sortOrder = 2),
                    WalletEntity(name = "ACLEDA Bank", type = WalletType.ACLEDA, initialBalance = BigDecimal.ZERO, currentBalance = BigDecimal.ZERO, currency = "USD", sortOrder = 3),
                    WalletEntity(name = "Wing Bank", type = WalletType.WING, initialBalance = BigDecimal.ZERO, currentBalance = BigDecimal.ZERO, currency = "USD", sortOrder = 4)
                )
                walletDao.insertAll(defaultWallets)
            }
        }
    }
}
