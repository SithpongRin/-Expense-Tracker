package com.expensetracker.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expensetracker.app.R
import com.expensetracker.app.ui.screens.budgets.BudgetsScreen
import com.expensetracker.app.ui.screens.budgets.BudgetsViewModel
import com.expensetracker.app.ui.screens.dashboard.DashboardScreen
import com.expensetracker.app.ui.screens.dashboard.DashboardViewModel
import com.expensetracker.app.ui.screens.debts.DebtsScreen
import com.expensetracker.app.ui.screens.debts.DebtsViewModel
import com.expensetracker.app.ui.screens.entry.TransactionEntryBottomSheet
import com.expensetracker.app.ui.screens.entry.TransactionEntryViewModel
import com.expensetracker.app.ui.screens.recurring.RecurringScreen
import com.expensetracker.app.ui.screens.recurring.RecurringViewModel
import com.expensetracker.app.ui.screens.reports.ReportsScreen
import com.expensetracker.app.ui.screens.reports.ReportsViewModel
import com.expensetracker.app.ui.screens.savings.SavingsScreen
import com.expensetracker.app.ui.screens.savings.SavingsViewModel
import com.expensetracker.app.ui.screens.settings.SettingsScreen
import com.expensetracker.app.ui.screens.settings.SettingsViewModel
import com.expensetracker.app.ui.screens.transactions.TransactionsScreen
import com.expensetracker.app.ui.screens.transactions.TransactionsViewModel
import com.expensetracker.app.ui.screens.wallets.WalletsScreen
import com.expensetracker.app.ui.screens.wallets.WalletsViewModel

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object Transactions : Screen("transactions", R.string.nav_transactions, Icons.AutoMirrored.Filled.List)
    object Reports : Screen("reports", R.string.nav_reports, Icons.Default.Assessment)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)

    // Sub-screens
    object Budgets : Screen("budgets", R.string.budgets_title, Icons.Default.Home)
    object Wallets : Screen("wallets", R.string.wallet_management, Icons.Default.Home)
    object Savings : Screen("savings", R.string.savings_goals, Icons.Default.Home)
    object Debts : Screen("debts", R.string.debt_tracker, Icons.Default.Home)
    object Recurring : Screen("recurring", R.string.recurring_title, Icons.Default.Home)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    transactionsViewModel: TransactionsViewModel = hiltViewModel(),
    reportsViewModel: ReportsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    entryViewModel: TransactionEntryViewModel = hiltViewModel()
) {
    var showEntryBottomSheet by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Transactions,
        Screen.Reports,
        Screen.Settings
    )

    val isTopLevelDestination = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTopLevelDestination) {
                Box(contentAlignment = Alignment.BottomCenter) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        // Home
                        NavigationBarItem(
                            icon = { Icon(Screen.Home.icon, contentDescription = null) },
                            label = { Text(stringResource(Screen.Home.titleRes)) },
                            selected = currentRoute == Screen.Home.route,
                            onClick = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Transactions
                        NavigationBarItem(
                            icon = { Icon(Screen.Transactions.icon, contentDescription = null) },
                            label = { Text(stringResource(Screen.Transactions.titleRes)) },
                            selected = currentRoute == Screen.Transactions.route,
                            onClick = {
                                navController.navigate(Screen.Transactions.route) {
                                    popUpTo(Screen.Home.route)
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Spacer placeholder for the prominent central Add button
                        NavigationBarItem(
                            icon = { Box(modifier = Modifier.size(24.dp)) },
                            label = { Text("") },
                            selected = false,
                            onClick = {
                                entryViewModel.initForCreate()
                                showEntryBottomSheet = true
                            },
                            enabled = false
                        )

                        // Reports
                        NavigationBarItem(
                            icon = { Icon(Screen.Reports.icon, contentDescription = null) },
                            label = { Text(stringResource(Screen.Reports.titleRes)) },
                            selected = currentRoute == Screen.Reports.route,
                            onClick = {
                                navController.navigate(Screen.Reports.route) {
                                    popUpTo(Screen.Home.route)
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Settings
                        NavigationBarItem(
                            icon = { Icon(Screen.Settings.icon, contentDescription = null) },
                            label = { Text(stringResource(Screen.Settings.titleRes)) },
                            selected = currentRoute == Screen.Settings.route,
                            onClick = {
                                navController.navigate(Screen.Settings.route) {
                                    popUpTo(Screen.Home.route)
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Prominent Central Floating Add Button
                    Box(
                        modifier = Modifier
                            .offset(y = (-20).dp)
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .clickable {
                                entryViewModel.initForCreate()
                                showEntryBottomSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.nav_add),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                    onTransactionClick = { transactionId ->
                        entryViewModel.initForEdit(transactionId)
                        showEntryBottomSheet = true
                    },
                    onOpenAddSheet = {
                        entryViewModel.initForCreate()
                        showEntryBottomSheet = true
                    }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    viewModel = transactionsViewModel,
                    onEditTransaction = { transactionId ->
                        entryViewModel.initForEdit(transactionId)
                        showEntryBottomSheet = true
                    }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(viewModel = reportsViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                    onNavigateToWallets = { navController.navigate(Screen.Wallets.route) },
                    onNavigateToSavings = { navController.navigate(Screen.Savings.route) },
                    onNavigateToDebts = { navController.navigate(Screen.Debts.route) },
                    onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) }
                )
            }

            composable(Screen.Budgets.route) {
                val bViewModel: BudgetsViewModel = hiltViewModel()
                BudgetsScreen(
                    viewModel = bViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Wallets.route) {
                val wViewModel: WalletsViewModel = hiltViewModel()
                WalletsScreen(
                    viewModel = wViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Savings.route) {
                val sViewModel: SavingsViewModel = hiltViewModel()
                SavingsScreen(
                    viewModel = sViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Debts.route) {
                val dViewModel: DebtsViewModel = hiltViewModel()
                DebtsScreen(
                    viewModel = dViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Recurring.route) {
                val rViewModel: RecurringViewModel = hiltViewModel()
                RecurringScreen(
                    viewModel = rViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }

    // Modal Bottom Sheet Transaction Entry
    if (showEntryBottomSheet) {
        TransactionEntryBottomSheet(
            viewModel = entryViewModel,
            onDismissRequest = {
                showEntryBottomSheet = false
                dashboardViewModel.refresh()
            }
        )
    }
}
