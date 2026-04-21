package com.docvault.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docvault.app.preferences.AppPreferences
import com.docvault.app.preferences.AppPreferencesRepository
import com.docvault.app.security.PinHasher
import com.docvault.app.security.hasFingerprintSensor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class AppSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val deferLockClearForFilePicker = AtomicBoolean(false)

    fun setDeferLockClearForFilePicker(defer: Boolean) {
        deferLockClearForFilePicker.set(defer)
    }

    fun shouldDeferLockClearForFilePicker(): Boolean =
        deferLockClearForFilePicker.get()

    private val repository = AppPreferencesRepository(application)

    val preferences = repository.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppPreferences()
    )

    private val _sessionUnlocked = MutableStateFlow(false)
    val sessionUnlocked: StateFlow<Boolean> = _sessionUnlocked.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    fun unlockSession() {
        _sessionUnlocked.value = true
    }

    fun clearUnlockSession() {
        _sessionUnlocked.value = false
    }

    fun openSettings() {
        _showSettings.value = true
    }

    fun closeSettings() {
        _showSettings.value = false
    }

    fun verifyPin(pin: String, prefs: AppPreferences): Boolean {
        val h = prefs.pinHash ?: return false
        val s = prefs.salt ?: return false
        return PinHasher.verify(pin, s, h)
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkTheme(enabled) }
    }

    fun setLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setLockEnabled(enabled)
            if (!enabled) {
                repository.setFingerprintUnlock(false)
                repository.clearPinCredentials()
            }
        }
    }

    fun saveNewPin(pin: String) {
        viewModelScope.launch {
            val salt = UUID.randomUUID().toString()
            val hash = PinHasher.hash(pin, salt)
            repository.setPinCredentials(hash, salt)
            repository.setLockEnabled(true)
            if (!getApplication<Application>().hasFingerprintSensor()) {
                repository.setFingerprintUnlock(false)
            }
        }
    }

    fun setFingerprintUnlock(enabled: Boolean) {
        if (!getApplication<Application>().hasFingerprintSensor()) return
        viewModelScope.launch { repository.setFingerprintUnlock(enabled) }
    }

    fun clearPinAndLock() {
        viewModelScope.launch {
            repository.clearPinCredentials()
            repository.setFingerprintUnlock(false)
            repository.setLockEnabled(false)
        }
    }
}
