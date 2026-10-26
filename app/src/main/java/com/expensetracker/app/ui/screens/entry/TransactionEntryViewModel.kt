package com.expensetracker.app.ui.screens.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.core.util.CalculatorEngine
import com.expensetracker.app.core.util.DateTimeUtils
import com.expensetracker.app.core.util.OcrReceiptResult
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.data.local.entity.WalletEntity
import com.expensetracker.app.data.preferences.UserPreferencesRepository
import com.expensetracker.app.data.repository.CategoryRepository
import com.expensetracker.app.data.repository.TransactionRepository
import com.expensetracker.app.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class TransactionEntryUiState(
    val expression: String = "",
    val evaluatedAmount: BigDecimal = BigDecimal.ZERO,
    val type: TransactionType = TransactionType.EXPENSE,
    val currency: String = "USD",
    val selectedCategoryId: Long? = null,
    val selectedWalletId: Long = 1L,
    val selectedToWalletId: Long? = null,
    val dateMillis: Long = DateTimeUtils.nowMillis(),
    val merchant: String = "",
    val notes: String = "",
    val receiptImagePath: String? = null,
    val isEditing: Boolean = false,
    val editingId: Long? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TransactionEntryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val calculator = CalculatorEngine()

    private val _uiState = MutableStateFlow(TransactionEntryUiState())
    val uiState: StateFlow<TransactionEntryUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllActiveCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wallets: StateFlow<List<WalletEntity>> = walletRepository.getAllActiveWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            _uiState.value = _uiState.value.copy(
                currency = prefs.defaultCurrency,
                selectedWalletId = prefs.defaultWalletId
            )
        }
    }

    fun initForCreate(initialType: TransactionType = TransactionType.EXPENSE) {
        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            val cats = categoryRepository.getAllActiveCategories().first()
            val defaultCat = cats.firstOrNull { it.type == initialType }?.id

            _uiState.value = TransactionEntryUiState(
                type = initialType,
                currency = prefs.defaultCurrency,
                selectedWalletId = prefs.defaultWalletId,
                selectedCategoryId = defaultCat,
                dateMillis = DateTimeUtils.nowMillis()
            )
        }
    }

    fun initForEdit(transactionId: Long) {
        viewModelScope.launch {
            val details = transactionRepository.getTransactionById(transactionId) ?: return@launch
            val t = details.transaction
            _uiState.value = TransactionEntryUiState(
                expression = t.amount.toPlainString(),
                evaluatedAmount = t.amount,
                type = t.type,
                currency = t.currency,
                selectedCategoryId = t.categoryId,
                selectedWalletId = t.walletId,
                selectedToWalletId = t.toWalletId,
                dateMillis = t.date,
                merchant = t.merchant,
                notes = t.notes,
                receiptImagePath = t.receiptImagePath,
                isEditing = true,
                editingId = t.id
            )
        }
    }

    fun onTypeChange(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type)
        // Auto-select first matching category
        val currentCats = categories.value
        val matchCat = currentCats.firstOrNull { it.type == type }?.id
        if (matchCat != null) {
            _uiState.value = _uiState.value.copy(selectedCategoryId = matchCat)
        }
    }

    fun onDigitClick(digit: String) {
        val current = _uiState.value.expression
        val updated = current + digit
        recalculate(updated)
    }

    fun onOperatorClick(op: String) {
        val current = _uiState.value.expression
        if (current.isEmpty()) return
        val updated = "$current $op "
        _uiState.value = _uiState.value.copy(expression = updated)
    }

    fun onDecimalClick() {
        val current = _uiState.value.expression
        val updated = if (current.isEmpty()) "0." else "$current."
        recalculate(updated)
    }

    fun onClearClick() {
        _uiState.value = _uiState.value.copy(
            expression = "",
            evaluatedAmount = BigDecimal.ZERO,
            errorMessage = null
        )
    }

    fun onBackspaceClick() {
        val current = _uiState.value.expression
        if (current.isNotEmpty()) {
            val updated = current.trimEnd().dropLast(1).trimEnd()
            recalculate(updated)
        }
    }

    fun onEqualsClick() {
        try {
            val result = calculator.evaluate(_uiState.value.expression)
            _uiState.value = _uiState.value.copy(
                expression = result.toPlainString(),
                evaluatedAmount = result,
                errorMessage = null
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Invalid expression")
        }
    }

    private fun recalculate(expr: String) {
        try {
            val result = calculator.evaluate(expr)
            _uiState.value = _uiState.value.copy(
                expression = expr,
                evaluatedAmount = result,
                errorMessage = null
            )
        } catch (e: Exception) {
            // Keep current expression while user is typing incomplete expression
            _uiState.value = _uiState.value.copy(expression = expr)
        }
    }

    fun onCategorySelect(categoryId: Long) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun onWalletSelect(walletId: Long) {
        _uiState.value = _uiState.value.copy(selectedWalletId = walletId)
    }

    fun onToWalletSelect(toWalletId: Long?) {
        _uiState.value = _uiState.value.copy(selectedToWalletId = toWalletId)
    }

    fun onDateChange(dateMillis: Long) {
        _uiState.value = _uiState.value.copy(dateMillis = dateMillis)
    }

    fun onMerchantChange(merchant: String) {
        _uiState.value = _uiState.value.copy(merchant = merchant)
    }

    fun onNotesChange(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun onReceiptImageAttached(imagePath: String?) {
        _uiState.value = _uiState.value.copy(receiptImagePath = imagePath)
    }

    fun applyOcrResult(result: OcrReceiptResult) {
        val amount = result.detectedAmount
        val merchant = result.detectedMerchant
        val date = result.detectedDateMillis

        _uiState.value = _uiState.value.copy(
            expression = amount?.toPlainString() ?: _uiState.value.expression,
            evaluatedAmount = amount ?: _uiState.value.evaluatedAmount,
            merchant = merchant ?: _uiState.value.merchant,
            dateMillis = date ?: _uiState.value.dateMillis
        )
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val state = _uiState.value
        val amount = state.evaluatedAmount

        if (amount <= BigDecimal.ZERO) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter an amount greater than zero.")
            return
        }

        if (state.type == TransactionType.TRANSFER && state.selectedToWalletId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select a destination wallet for transfer.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                if (state.isEditing && state.editingId != null) {
                    val updated = TransactionEntity(
                        id = state.editingId,
                        type = state.type,
                        amount = amount,
                        currency = state.currency,
                        categoryId = if (state.type == TransactionType.TRANSFER) null else state.selectedCategoryId,
                        walletId = state.selectedWalletId,
                        toWalletId = if (state.type == TransactionType.TRANSFER) state.selectedToWalletId else null,
                        date = state.dateMillis,
                        merchant = state.merchant.trim(),
                        notes = state.notes.trim(),
                        receiptImagePath = state.receiptImagePath,
                        updatedAt = System.currentTimeMillis()
                    )
                    transactionRepository.updateTransaction(updated)
                } else {
                    val newEntity = TransactionEntity(
                        type = state.type,
                        amount = amount,
                        currency = state.currency,
                        categoryId = if (state.type == TransactionType.TRANSFER) null else state.selectedCategoryId,
                        walletId = state.selectedWalletId,
                        toWalletId = if (state.type == TransactionType.TRANSFER) state.selectedToWalletId else null,
                        date = state.dateMillis,
                        merchant = state.merchant.trim(),
                        notes = state.notes.trim(),
                        receiptImagePath = state.receiptImagePath
                    )
                    transactionRepository.addTransaction(newEntity)
                }
                _uiState.value = _uiState.value.copy(isSaving = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = e.message ?: "Failed to save transaction")
            }
        }
    }
}
