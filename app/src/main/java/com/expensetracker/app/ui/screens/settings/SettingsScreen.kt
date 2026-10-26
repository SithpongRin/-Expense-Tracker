package com.expensetracker.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.BuildConfig
import com.expensetracker.app.R
import com.expensetracker.app.core.model.AppLanguage
import com.expensetracker.app.core.model.AppTheme
import com.expensetracker.app.core.model.Currency
import com.expensetracker.app.ui.components.MonochromeCard

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToBudgets: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onNavigateToSavings: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToRecurring: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateError by viewModel.updateError.collectAsState()

    val context = LocalContext.current

    var showPasswordDialogForBackup by remember { mutableStateOf(false) }
    var showPasswordDialogForRestore by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var restoreBytes by remember { mutableStateOf<ByteArray?>(null) }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                restoreBytes = bytes
                showPasswordDialogForRestore = true
            }
        }
    }

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
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Modules Management Section
        item {
            Text(
                text = "Financial Modules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            MonochromeCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                SettingsNavigationRow(
                    icon = Icons.Default.PieChart,
                    title = stringResource(R.string.budgets_title),
                    onClick = onNavigateToBudgets
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Default.AccountBalance,
                    title = stringResource(R.string.wallet_management),
                    onClick = onNavigateToWallets
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Default.Savings,
                    title = stringResource(R.string.savings_goals),
                    onClick = onNavigateToSavings
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Default.Money,
                    title = stringResource(R.string.debt_tracker),
                    onClick = onNavigateToDebts
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Default.Repeat,
                    title = stringResource(R.string.recurring_title),
                    onClick = onNavigateToRecurring
                )
            }
        }

        // Preferences Section
        item {
            Text(
                text = stringResource(R.string.settings_general),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            MonochromeCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp) {
                // Language Selector
                var langMenuExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { langMenuExpanded = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        text = prefs.language.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    DropdownMenu(
                        expanded = langMenuExpanded,
                        onDismissRequest = { langMenuExpanded = false }
                    ) {
                        AppLanguage.values().forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.displayName) },
                                onClick = {
                                    viewModel.setLanguage(lang)
                                    langMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Appearance Selector (Light, Dark, System)
                var themeMenuExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { themeMenuExpanded = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        text = when (prefs.theme) {
                            AppTheme.LIGHT -> stringResource(R.string.theme_light)
                            AppTheme.DARK -> stringResource(R.string.theme_dark)
                            AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    DropdownMenu(
                        expanded = themeMenuExpanded,
                        onDismissRequest = { themeMenuExpanded = false }
                    ) {
                        AppTheme.values().forEach { themeOption ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (themeOption) {
                                            AppTheme.LIGHT -> stringResource(R.string.theme_light)
                                            AppTheme.DARK -> stringResource(R.string.theme_dark)
                                            AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                                        }
                                    )
                                },
                                onClick = {
                                    viewModel.setTheme(themeOption)
                                    themeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Default Currency
                var currMenuExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { currMenuExpanded = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Money, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.settings_currency), style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        text = prefs.defaultCurrency,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    DropdownMenu(
                        expanded = currMenuExpanded,
                        onDismissRequest = { currMenuExpanded = false }
                    ) {
                        Currency.values().forEach { curr ->
                            DropdownMenuItem(
                                text = { Text(curr.displayName) },
                                onClick = {
                                    viewModel.setDefaultCurrency(curr.code)
                                    currMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Biometric Lock Toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.settings_biometric_lock), style = MaterialTheme.typography.bodyLarge)
                    }
                    Switch(
                        checked = prefs.isBiometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) }
                    )
                }
            }
        }

        // Backup & Restore Section
        item {
            Text(
                text = stringResource(R.string.settings_backup_export),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            MonochromeCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp) {
                Button(
                    onClick = {
                        backupPassword = ""
                        showPasswordDialogForBackup = true
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.backup_full), fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { restoreFileLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(stringResource(R.string.restore_full), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // GitHub Updates Section
        item {
            Text(
                text = stringResource(R.string.settings_updates),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            MonochromeCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${stringResource(R.string.app_version)} ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Build ${BuildConfig.VERSION_CODE}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { viewModel.checkForUpdates() },
                        enabled = !isCheckingUpdate,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.check_updates))
                        }
                    }
                }

                if (updateError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = updateError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (updateInfo != null) {
                    val info = updateInfo!!
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (info.hasUpdate) "Update Available: v${info.latestVersionName}" else "App is up to date (v${info.latestVersionName})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = info.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Backup Password Dialog
    if (showPasswordDialogForBackup) {
        AlertDialog(
            onDismissRequest = { showPasswordDialogForBackup = false },
            title = { Text(stringResource(R.string.backup_full)) },
            text = {
                Column {
                    Text("Enter a secure passphrase to encrypt your financial backup:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (backupPassword.isNotBlank()) {
                            viewModel.createBackup(backupPassword, context) { intent ->
                                context.startActivity(intent)
                            }
                            showPasswordDialogForBackup = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialogForBackup = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Restore Password Dialog
    if (showPasswordDialogForRestore && restoreBytes != null) {
        AlertDialog(
            onDismissRequest = { showPasswordDialogForRestore = false },
            title = { Text(stringResource(R.string.restore_full)) },
            text = {
                Column {
                    Text("Enter the password used to encrypt this backup:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (backupPassword.isNotBlank()) {
                            viewModel.restoreBackup(restoreBytes!!, backupPassword)
                            showPasswordDialogForRestore = false
                            restoreBytes = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialogForRestore = false
                    restoreBytes = null
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    )
}
