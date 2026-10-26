package com.expensetracker.app.ui.screens.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.model.DebtType
import com.expensetracker.app.data.local.entity.DebtEntity
import com.expensetracker.app.data.local.entity.DebtRepaymentEntity
import com.expensetracker.app.data.local.entity.WalletEntity
import com.expensetracker.app.data.repository.DebtRepository
import com.expensetracker.app.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    val debts: StateFlow<List<DebtEntity>> = debtRepository.getAllDebts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wallets: StateFlow<List<WalletEntity>> = walletRepository.getAllActiveWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addDebt(type: DebtType, personName: String, amount: BigDecimal, notes: String = "") {
        viewModelScope.launch {
            val debt = DebtEntity(
                type = type,
                personName = personName,
                originalAmount = amount,
                remainingAmount = amount,
                notes = notes
            )
            debtRepository.addDebt(debt)
        }
    }

    fun addRepayment(debtId: Long, amount: BigDecimal, walletId: Long?) {
        viewModelScope.launch {
            val repayment = DebtRepaymentEntity(
                debtId = debtId,
                walletId = walletId,
                amount = amount
            )
            debtRepository.addRepayment(repayment)
        }
    }
}
