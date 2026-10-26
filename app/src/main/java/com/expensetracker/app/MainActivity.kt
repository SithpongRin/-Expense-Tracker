package com.expensetracker.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.expensetracker.app.core.model.AppLanguage
import com.expensetracker.app.core.util.BiometricPromptHelper
import com.expensetracker.app.core.util.NotificationHelper
import com.expensetracker.app.data.preferences.UserPreferences
import com.expensetracker.app.data.preferences.UserPreferencesRepository
import com.expensetracker.app.ui.navigation.AppNavigation
import com.expensetracker.app.ui.theme.ExpenseTrackerTheme
import com.expensetracker.app.worker.WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    private var isUnlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannels(this)
        WorkScheduler.scheduleRecurringTransactionWorker(this)

        lifecycleScope.launch {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            applyLocale(prefs.language)

            if (!prefs.isBiometricEnabled || !BiometricPromptHelper.isBiometricAvailable(this@MainActivity)) {
                isUnlocked = true
            } else {
                authenticateBiometric()
            }
        }

        setContent {
            val prefs by preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())

            ExpenseTrackerTheme(appTheme = prefs.theme) {
                if (isUnlocked) {
                    AppNavigation()
                } else {
                    BiometricLockScreen(
                        onUnlockClick = { authenticateBiometric() }
                    )
                }
            }
        }
    }

    private fun applyLocale(language: AppLanguage) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun authenticateBiometric() {
        BiometricPromptHelper.showBiometricPrompt(
            activity = this,
            title = "Expense Tracker Security",
            subtitle = "Verify your identity to unlock",
            onSuccess = {
                isUnlocked = true
            },
            onError = { _ ->
                isUnlocked = false
            }
        )
    }
}

@Composable
fun BiometricLockScreen(onUnlockClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Locked",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Expense Tracker Locked",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onUnlockClick) {
                Text("Unlock")
            }
        }
    }
}
