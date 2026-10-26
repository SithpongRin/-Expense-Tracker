package com.expensetracker.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.core.util.DateTimeUtils
import com.expensetracker.app.data.local.entity.CategoryTotal
import com.expensetracker.app.data.local.entity.TransactionWithDetails
import com.expensetracker.app.data.preferences.UserPreferencesRepository
import com.expensetracker.app.data.repository.BudgetProgress
import com.expensetracker.app.data.repository.BudgetRepository
import com.expensetracker.app.data.repository.TransactionRepository
import com.expensetracker.app.data.repository.WalletRepository
import com.expensetracker.app.ui.components.TrendPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import javax.inject.Inject

data class DashboardUiState(
    val selectedYearMonth: YearMonth = YearMonth.now(DateTimeUtils.PHNOM_PENH_ZONE),
    val totalBalance: BigDecimal = BigDecimal.ZERO,
    val monthlyIncome: BigDecimal = BigDecimal.ZERO,
    val monthlyExpense: BigDecimal = BigDecimal.ZERO,
    val netSavings: BigDecimal = BigDecimal.ZERO,
    val currency: String = "USD",
    val budgetProgress: BudgetProgress? = null,
    val topCategories: List<CategoryTotal> = emptyList(),
    val recentTransactions: List<TransactionWithDetails> = emptyList(),
    val trendPoints: List<TrendPoint> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
    private val budgetRepository: BudgetRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now(DateTimeUtils.PHNOM_PENH_ZONE))
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
        loadDashboardData()
    }

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
        loadDashboardData()
    }

    fun refresh() {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            val ym = _selectedMonth.value
            val startMillis = DateTimeUtils.getStartOfMonth(ym)
            val endMillis = DateTimeUtils.getEndOfMonth(ym)
            val monthKey = DateTimeUtils.toMonthKey(ym)

            val prefs = preferencesRepository.userPreferencesFlow.first()
            val wallets = walletRepository.getAllActiveWallets().first()
            val totalBalance = wallets.fold(BigDecimal.ZERO) { acc, w -> acc.add(w.currentBalance) }

            // Monthly income & expense
            val incomeDouble = transactionRepository.getTotalByTypeAndDateRange(
                TransactionType.INCOME, startMillis, endMillis
            ).first()
            val expenseDouble = transactionRepository.getTotalByTypeAndDateRange(
                TransactionType.EXPENSE, startMillis, endMillis
            ).first()

            val monthlyIncome = BigDecimal(incomeDouble).setScale(2, RoundingMode.HALF_EVEN)
            val monthlyExpense = BigDecimal(expenseDouble).setScale(2, RoundingMode.HALF_EVEN)
            val netSavings = monthlyIncome.subtract(monthlyExpense)

            // Budget
            val budgetProgress = budgetRepository.calculateBudgetProgress(monthKey)

            // Top categories
            val topCategories = transactionRepository.getTopExpenseCategories(startMillis, endMillis).first()

            // Recent transactions
            val recent = transactionRepository.getRecentWithDetailsFlow(6).first()

            // Trend points (days in month)
            val transactionsInMonth = transactionRepository.getWithDetailsByDateRange(startMillis, endMillis).first()
            val trendMap = transactionsInMonth
                .filter { it.transaction.type == TransactionType.EXPENSE }
                .groupBy { DateTimeUtils.formatDayMonth(it.transaction.date) }
                .mapValues { entry -> entry.value.sumOf { it.transaction.amount.toDouble() }.toFloat() }

            val points = trendMap.entries.map { TrendPoint(it.key, it.value) }.takeLast(7)

            _uiState.value = DashboardUiState(
                selectedYearMonth = ym,
                totalBalance = totalBalance,
                monthlyIncome = monthlyIncome,
                monthlyExpense = monthlyExpense,
                netSavings = netSavings,
                currency = prefs.defaultCurrency,
                budgetProgress = budgetProgress,
                topCategories = topCategories,
                recentTransactions = recent,
                trendPoints = points,
                isLoading = false
            )
        }
    }
}
