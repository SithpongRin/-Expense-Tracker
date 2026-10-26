package com.expensetracker.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.model.AppLanguage
import com.expensetracker.app.core.model.AppTheme
import com.expensetracker.app.core.model.Currency
import com.expensetracker.app.data.preferences.UserPreferences
import com.expensetracker.app.data.preferences.UserPreferencesRepository
import com.expensetracker.app.data.repository.BackupRepository
import com.expensetracker.app.data.repository.UpdateInfo
import com.expensetracker.app.data.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class BackupState {
    object Idle : BackupState()
    object Processing : BackupState()
    data class ExportSuccess(val backupFile: File) : BackupState()
    object RestoreSuccess : BackupState()
    data class Error(val message: String) : BackupState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val backupRepository: BackupRepository,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateError = MutableStateFlow<String?>(null)
    val updateError: StateFlow<String?> = _updateError.asStateFlow()

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { preferencesRepository.setTheme(theme) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
    }

    fun setDefaultCurrency(currency: String) {
        viewModelScope.launch { preferencesRepository.setDefaultCurrency(currency) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setBiometricEnabled(enabled) }
    }

    fun createBackup(password: String, context: Context, onReadyToShare: (Intent) -> Unit) {
        viewModelScope.launch {
            _backupState.value = BackupState.Processing
            try {
                val file = backupRepository.createEncryptedBackup(password)
                _backupState.value = BackupState.ExportSuccess(file)

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                onReadyToShare(Intent.createChooser(shareIntent, "Share Encrypted Backup"))
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "Failed to create encrypted backup")
            }
        }
    }

    fun restoreBackup(bytes: ByteArray, password: String) {
        viewModelScope.launch {
            _backupState.value = BackupState.Processing
            try {
                val success = backupRepository.restoreEncryptedBackup(bytes, password)
                if (success) {
                    _backupState.value = BackupState.RestoreSuccess
                } else {
                    _backupState.value = BackupState.Error("Failed to restore backup")
                }
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "Invalid password or corrupted backup")
            }
        }
    }

    fun resetBackupState() {
        _backupState.value = BackupState.Idle
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _updateError.value = null
            val result = updateRepository.checkForUpdates()
            result.onSuccess { info ->
                _updateInfo.value = info
                _isCheckingUpdate.value = false
            }.onFailure { err ->
                _updateError.value = err.message ?: "Failed to check for updates"
                _isCheckingUpdate.value = false
            }
        }
    }

    fun clearUpdateInfo() {
        _updateInfo.value = null
        _updateError.value = null
    }
}
