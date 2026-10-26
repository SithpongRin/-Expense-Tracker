package com.expensetracker.app.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.R
import com.expensetracker.app.core.util.CurrencyFormatter
import com.expensetracker.app.ui.components.CategoryBreakdownBar
import com.expensetracker.app.ui.components.MonochromeCard
import com.expensetracker.app.ui.components.SpendingTrendChart

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val periods = listOf(
        Pair(ReportPeriod.DAILY, stringResource(R.string.period_daily)),
        Pair(ReportPeriod.WEEKLY, stringResource(R.string.period_weekly)),
        Pair(ReportPeriod.MONTHLY, stringResource(R.string.period_monthly)),
        Pair(ReportPeriod.YEARLY, stringResource(R.string.period_yearly))
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.reports_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Period Tab Row
        item {
            val selectedIndex = periods.indexOfFirst { it.first == uiState.selectedPeriod }.coerceAtLeast(0)
            TabRow(
                selectedTabIndex = selectedIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                periods.forEachIndexed { index, pair ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { viewModel.onPeriodSelect(pair.first) },
                        text = {
                            Text(
                                text = pair.second,
                                fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }

        // Net Savings Summary Card
        item {
            MonochromeCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.net_savings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = CurrencyFormatter.format(
                        amount = uiState.netSavings,
                        currencyCode = uiState.currency,
                        showPositiveSign = true
                    ),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Income: ${CurrencyFormatter.format(uiState.totalIncome, uiState.currency)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Expense: -${CurrencyFormatter.format(uiState.totalExpense, uiState.currency)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Spending Trends
        item {
            MonochromeCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.spending_trends),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                SpendingTrendChart(
                    points = uiState.trendPoints,
                    currencyCode = uiState.currency
                )
            }
        }

        // Category Breakdown
        item {
            MonochromeCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.breakdown_by_category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (uiState.categoryTotals.isEmpty()) {
                    Text(
                        text = "No category data available for this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val totalExpenses = uiState.totalExpense.max(java.math.BigDecimal.ONE)
                    uiState.categoryTotals.forEach { catTotal ->
                        val percent = (catTotal.totalAmount.toDouble() / totalExpenses.toDouble() * 100.0).toFloat()
                        CategoryBreakdownBar(
                            categoryName = catTotal.categoryName ?: "Other",
                            amount = catTotal.totalAmount,
                            percentage = percent,
                            currencyCode = uiState.currency
                        )
                    }
                }
            }
        }

        // Export Actions (CSV and PDF)
        item {
            MonochromeCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Export Statement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.exportCsv(context) { intent -> context.startActivity(intent) } },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.export_csv))
                    }

                    Button(
                        onClick = { viewModel.exportPdf(context) { intent -> context.startActivity(intent) } },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.export_pdf))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
