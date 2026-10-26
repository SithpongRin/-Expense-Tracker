package com.expensetracker.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionWithDetails(
    @Embedded
    val transaction: TransactionEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?,

    @Relation(
        parentColumn = "walletId",
        entityColumn = "id"
    )
    val wallet: WalletEntity,

    @Relation(
        parentColumn = "toWalletId",
        entityColumn = "id"
    )
    val toWallet: WalletEntity?
)

data class CategoryTotal(
    val categoryId: Long?,
    val categoryName: String?,
    val totalAmount: java.math.BigDecimal,
    val count: Int
)

data class PeriodSummary(
    val totalIncome: java.math.BigDecimal,
    val totalExpense: java.math.BigDecimal
)
