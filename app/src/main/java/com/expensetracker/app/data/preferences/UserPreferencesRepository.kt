package com.expensetracker.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.expensetracker.app.core.model.AppLanguage
import com.expensetracker.app.core.model.AppTheme
import com.expensetracker.app.core.model.Currency
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val defaultCurrency: String = Currency.USD.code,
    val defaultWalletId: Long = 1L,
    val isBiometricEnabled: Boolean = false,
    val isBudgetAlertEnabled: Boolean = true,
    val isDailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 20, // 8:00 PM
    val dailyReminderMinute: Int = 0
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val THEME = stringPreferencesKey("app_theme")
        val LANGUAGE = stringPreferencesKey("app_language")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val DEFAULT_WALLET_ID = longPreferencesKey("default_wallet_id")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val BUDGET_ALERT_ENABLED = booleanPreferencesKey("budget_alert_enabled")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME] ?: AppTheme.SYSTEM.name
        val langStr = prefs[Keys.LANGUAGE] ?: AppLanguage.ENGLISH.code
        val currencyStr = prefs[Keys.DEFAULT_CURRENCY] ?: Currency.USD.code
        val walletId = prefs[Keys.DEFAULT_WALLET_ID] ?: 1L
        val biometric = prefs[Keys.BIOMETRIC_ENABLED] ?: false
        val budgetAlert = prefs[Keys.BUDGET_ALERT_ENABLED] ?: true
        val dailyReminder = prefs[Keys.DAILY_REMINDER_ENABLED] ?: false
        val reminderHour = prefs[Keys.DAILY_REMINDER_HOUR] ?: 20
        val reminderMinute = prefs[Keys.DAILY_REMINDER_MINUTE] ?: 0

        UserPreferences(
            theme = try { AppTheme.valueOf(themeStr) } catch (e: Exception) { AppTheme.SYSTEM },
            language = if (langStr == "km") AppLanguage.KHMER else AppLanguage.ENGLISH,
            defaultCurrency = currencyStr,
            defaultWalletId = walletId,
            isBiometricEnabled = biometric,
            isBudgetAlertEnabled = budgetAlert,
            isDailyReminderEnabled = dailyReminder,
            dailyReminderHour = reminderHour,
            dailyReminderMinute = reminderMinute
        )
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME] = theme.name
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun setDefaultCurrency(currency: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_CURRENCY] = currency
        }
    }

    suspend fun setDefaultWalletId(walletId: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_WALLET_ID] = walletId
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setBudgetAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BUDGET_ALERT_ENABLED] = enabled
        }
    }

    suspend fun setDailyReminder(enabled: Boolean, hour: Int = 20, minute: Int = 0) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_REMINDER_ENABLED] = enabled
            prefs[Keys.DAILY_REMINDER_HOUR] = hour
            prefs[Keys.DAILY_REMINDER_MINUTE] = minute
        }
    }
}
