package com.expensetracker.app.ui.screens.reports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.core.util.CsvExporter
import com.expensetracker.app.core.util.DateTimeUtils
import com.expensetracker.app.core.util.PdfReportGenerator
import com.expensetracker.app.data.local.entity.CategoryTotal
import com.expensetracker.app.data.local.entity.TransactionWithDetails
import com.expensetracker.app.data.preferences.UserPreferencesRepository
import com.expensetracker.app.data.repository.TransactionRepository
import com.expensetracker.app.ui.components.TrendPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class ReportPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTHLY,
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalExpense: BigDecimal = BigDecimal.ZERO,
    val netSavings: BigDecimal = BigDecimal.ZERO,
    val currency: String = "USD",
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val trendPoints: List<TrendPoint> = emptyList(),
    val transactions: List<TransactionWithDetails> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReport(ReportPeriod.MONTHLY)
    }

    fun onPeriodSelect(period: ReportPeriod) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        loadReport(period)
    }

    private fun loadReport(period: ReportPeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val now = LocalDate.now(DateTimeUtils.PHNOM_PENH_ZONE)
            val (startMillis, endMillis) = when (period) {
                ReportPeriod.DAILY -> {
                    val start = DateTimeUtils.toEpochMillis(now)
                    val end = DateTimeUtils.toEpochMillis(now.plusDays(1)) - 1
                    Pair(start, end)
                }
                ReportPeriod.WEEKLY -> {
                    val start = DateTimeUtils.toEpochMillis(now.minusDays(6))
                    val end = DateTimeUtils.toEpochMillis(now.plusDays(1)) - 1
                    Pair(start, end)
                }
                ReportPeriod.MONTHLY -> {
                    val ym = YearMonth.from(now)
                    Pair(DateTimeUtils.getStartOfMonth(ym), DateTimeUtils.getEndOfMonth(ym))
                }
                ReportPeriod.YEARLY -> {
                    Pair(DateTimeUtils.getStartOfYear(now.year), DateTimeUtils.getEndOfYear(now.year))
                }
            }

            val prefs = preferencesRepository.userPreferencesFlow.first()
            val incomeDouble = transactionRepository.getTotalByTypeAndDateRange(TransactionType.INCOME, startMillis, endMillis).first()
            val expenseDouble = transactionRepository.getTotalByTypeAndDateRange(TransactionType.EXPENSE, startMillis, endMillis).first()

            val income = BigDecimal(incomeDouble).setScale(2, RoundingMode.HALF_EVEN)
            val expense = BigDecimal(expenseDouble).setScale(2, RoundingMode.HALF_EVEN)
            val net = income.subtract(expense)

            val categoryTotals = transactionRepository.getTopExpenseCategories(startMillis, endMillis).first()
            val transactions = transactionRepository.getWithDetailsByDateRange(startMillis, endMillis).first()

            val trendMap = transactions
                .filter { it.transaction.type == TransactionType.EXPENSE }
                .groupBy { DateTimeUtils.formatDayMonth(it.transaction.date) }
                .mapValues { entry -> entry.value.sumOf { it.transaction.amount.toDouble() }.toFloat() }

            val points = trendMap.entries.map { TrendPoint(it.key, it.value) }.takeLast(7)

            _uiState.value = _uiState.value.copy(
                totalIncome = income,
                totalExpense = expense,
                netSavings = net,
                currency = prefs.defaultCurrency,
                categoryTotals = categoryTotals,
                trendPoints = points,
                transactions = transactions,
                isLoading = false
            )
        }
    }

    fun exportPdf(context: Context, onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val pdfFile = PdfReportGenerator.generatePdfReport(
                context = context,
                title = "Expense Tracker Report",
                periodName = state.selectedPeriod.name,
                totalIncome = state.totalIncome,
                totalExpense = state.totalExpense,
                transactions = state.transactions
            )

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onReady(Intent.createChooser(intent, "Share PDF Report"))
        }
    }

    fun exportCsv(context: Context, onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val csvContent = CsvExporter.generateCsv(state.transactions)
            val csvFile = File(context.cacheDir, "transactions_${System.currentTimeMillis()}.csv")
            FileOutputStream(csvFile).use { out ->
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onReady(Intent.createChooser(intent, "Share CSV Export"))
        }
    }
}
