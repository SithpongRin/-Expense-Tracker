package com.expensetracker.app.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.TransactionWithDetails
import com.expensetracker.app.data.local.entity.WalletEntity
import com.expensetracker.app.data.repository.CategoryRepository
import com.expensetracker.app.data.repository.TransactionRepository
import com.expensetracker.app.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsFilterState(
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val selectedWalletId: Long? = null,
    val selectedCategoryId: Long? = null,
    val sortOrder: String = "NEWEST" // "NEWEST", "OLDEST", "HIGHEST", "LOWEST"
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(TransactionsFilterState())
    val filterState: StateFlow<TransactionsFilterState> = _filterState.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllActiveCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wallets: StateFlow<List<WalletEntity>> = walletRepository.getAllActiveWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionWithDetails>> = _filterState
        .flatMapLatest { filter ->
            transactionRepository.getFilteredTransactions(
                startDate = null,
                endDate = null,
                type = filter.selectedType,
                walletId = filter.selectedWalletId,
                categoryId = filter.selectedCategoryId,
                searchQuery = filter.searchQuery,
                sortOrder = filter.sortOrder
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun onTypeSelect(type: TransactionType?) {
        _filterState.value = _filterState.value.copy(selectedType = type)
    }

    fun onWalletSelect(walletId: Long?) {
        _filterState.value = _filterState.value.copy(selectedWalletId = walletId)
    }

    fun onCategorySelect(categoryId: Long?) {
        _filterState.value = _filterState.value.copy(selectedCategoryId = categoryId)
    }

    fun onSortOrderChange(order: String) {
        _filterState.value = _filterState.value.copy(sortOrder = order)
    }

    fun deleteTransaction(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
            onDone()
        }
    }
}
