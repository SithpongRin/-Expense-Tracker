package com.expensetracker.app.ui.screens.wallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.model.WalletType
import com.expensetracker.app.data.local.entity.WalletEntity
import com.expensetracker.app.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WalletsViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {

    val wallets: StateFlow<List<WalletEntity>> = walletRepository.getAllActiveWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWallet(name: String, type: WalletType, initialBalance: BigDecimal, currency: String = "USD") {
        viewModelScope.launch {
            val wallet = WalletEntity(
                name = name,
                type = type,
                initialBalance = initialBalance,
                currentBalance = initialBalance,
                currency = currency
            )
            walletRepository.addWallet(wallet)
        }
    }

    fun archiveWallet(id: Long) {
        viewModelScope.launch {
            walletRepository.archiveWallet(id)
        }
    }
}
