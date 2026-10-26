package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expensetracker.app.core.model.DebtType
import com.expensetracker.app.core.model.RecurringFrequency
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.core.model.WalletType
import java.math.BigDecimal

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = false)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val nameResKey: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val iconName: String = "category",
    val colorHex: String = "#000000",
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "wallets",
    indices = [Index(value = ["name"])]
)
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: WalletType = WalletType.CASH,
    val initialBalance: BigDecimal = BigDecimal.ZERO,
    val currentBalance: BigDecimal = BigDecimal.ZERO,
    val currency: String = "USD",
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["toWalletId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["walletId"]),
        Index(value = ["toWalletId"]),
        Index(value = ["date"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: BigDecimal,
    val currency: String = "USD",
    val categoryId: Long? = null,
    val walletId: Long,
    val toWalletId: Long? = null, // Used if type == TRANSFER
    val date: Long,              // Epoch millis (Asia/Phnom_Penh)
    val merchant: String = "",
    val notes: String = "",
    val receiptImagePath: String? = null,
    val isRecurring: Boolean = false,
    val recurringId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["monthKey", "categoryId"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long? = null, // null represents overall monthly budget
    val monthKey: String,         // "yyyy-MM" format
    val amount: BigDecimal,
    val warningThresholdPercent: Int = 80,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetAmount: BigDecimal,
    val currentAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "USD",
    val targetDate: Long? = null,
    val isCompleted: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "savings_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["walletId"])
    ]
)
data class SavingsContributionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    val walletId: Long? = null,
    val amount: BigDecimal,
    val isWithdrawal: Boolean = false,
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: DebtType, // I_OWE or THEY_OWE
    val personName: String,
    val originalAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val currency: String = "USD",
    val dueDate: Long? = null,
    val notes: String = "",
    val isSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "debt_repayments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["debtId"]),
        Index(value = ["walletId"])
    ]
)
data class DebtRepaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val debtId: Long,
    val walletId: Long? = null,
    val amount: BigDecimal,
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["walletId"]),
        Index(value = ["nextOccurrenceDate"])
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: BigDecimal,
    val currency: String = "USD",
    val categoryId: Long? = null,
    val walletId: Long,
    val merchant: String = "",
    val notes: String = "",
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val startDate: Long,
    val endDate: Long? = null,
    val nextOccurrenceDate: Long,
    val isActive: Boolean = true,
    val lastProcessedDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
