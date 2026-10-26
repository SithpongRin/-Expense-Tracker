package com.expensetracker.app.ui.screens.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.model.RecurringFrequency
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.core.util.DateTimeUtils
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.RecurringTransactionEntity
import com.expensetracker.app.data.local.entity.WalletEntity
import com.expensetracker.app.data.repository.CategoryRepository
import com.expensetracker.app.data.repository.RecurringRepository
import com.expensetracker.app.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    val recurringList: StateFlow<List<RecurringTransactionEntity>> = recurringRepository.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllActiveCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wallets: StateFlow<List<WalletEntity>> = walletRepository.getAllActiveWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRecurring(
        type: TransactionType,
        amount: BigDecimal,
        frequency: RecurringFrequency,
        merchant: String,
        notes: String,
        categoryId: Long?,
        walletId: Long
    ) {
        viewModelScope.launch {
            val now = DateTimeUtils.nowMillis()
            val nextDate = recurringRepository.calculateNextOccurrence(now, frequency)
            val entity = RecurringTransactionEntity(
                type = type,
                amount = amount,
                frequency = frequency,
                merchant = merchant,
                notes = notes,
                categoryId = categoryId,
                walletId = walletId,
                startDate = now,
                nextOccurrenceDate = nextDate,
                isActive = true
            )
            recurringRepository.addRecurring(entity)
        }
    }

    fun toggleActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            recurringRepository.toggleActive(id, isActive)
        }
    }

    fun deleteRecurring(entity: RecurringTransactionEntity) {
        viewModelScope.launch {
            recurringRepository.deleteRecurring(entity)
        }
    }
}
