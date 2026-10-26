package com.expensetracker.app.ui.screens.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.entity.SavingsContributionEntity
import com.expensetracker.app.data.local.entity.SavingsGoalEntity
import com.expensetracker.app.data.local.entity.WalletEntity
import com.expensetracker.app.data.repository.SavingsRepository
import com.expensetracker.app.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val savingsRepository: SavingsRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    val goals: StateFlow<List<SavingsGoalEntity>> = savingsRepository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wallets: StateFlow<List<WalletEntity>> = walletRepository.getAllActiveWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGoal(name: String, targetAmount: BigDecimal, currency: String = "USD") {
        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                name = name,
                targetAmount = targetAmount,
                currentAmount = BigDecimal.ZERO,
                currency = currency
            )
            savingsRepository.addGoal(goal)
        }
    }

    fun addContribution(goalId: Long, amount: BigDecimal, walletId: Long?) {
        viewModelScope.launch {
            val contribution = SavingsContributionEntity(
                goalId = goalId,
                amount = amount,
                walletId = walletId
            )
            savingsRepository.addContribution(contribution)
        }
    }
}
