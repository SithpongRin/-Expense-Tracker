package com.expensetracker.app.ui.screens.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.util.DateTimeUtils
import com.expensetracker.app.data.local.entity.BudgetEntity
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.preferences.UserPreferencesRepository
import com.expensetracker.app.data.repository.BudgetProgress
import com.expensetracker.app.data.repository.BudgetRepository
import com.expensetracker.app.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject

data class BudgetsUiState(
    val currentMonthKey: String = DateTimeUtils.toMonthKey(YearMonth.now(DateTimeUtils.PHNOM_PENH_ZONE)),
    val monthlyProgress: BudgetProgress? = null,
    val currency: String = "USD",
    val isLoading: Boolean = false
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    init {
        loadBudgets()
    }

    fun loadBudgets() {
        viewModelScope.launch {
            val ym = YearMonth.now(DateTimeUtils.PHNOM_PENH_ZONE)
            val monthKey = DateTimeUtils.toMonthKey(ym)
            val prefs = preferencesRepository.userPreferencesFlow.first()
            val progress = budgetRepository.calculateBudgetProgress(monthKey)

            _uiState.value = BudgetsUiState(
                currentMonthKey = monthKey,
                monthlyProgress = progress,
                currency = prefs.defaultCurrency
            )
        }
    }

    fun setMonthlyBudget(amount: BigDecimal, warningThreshold: Int = 80) {
        viewModelScope.launch {
            val ym = YearMonth.now(DateTimeUtils.PHNOM_PENH_ZONE)
            val monthKey = DateTimeUtils.toMonthKey(ym)
            val budget = BudgetEntity(
                categoryId = null,
                monthKey = monthKey,
                amount = amount,
                warningThresholdPercent = warningThreshold
            )
            budgetRepository.setBudget(budget)
            loadBudgets()
        }
    }
}
