package com.docvault.app.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "doc_vault_prefs")

class AppPreferencesRepository(private val context: Context) {

    private val dataStore get() = context.dataStore

    val preferences: Flow<AppPreferences> = dataStore.data.map { p ->
        AppPreferences(
            useDarkTheme = p[KEY_DARK_THEME] ?: false,
            lockEnabled = p[KEY_LOCK_ENABLED] ?: false,
            pinHash = p[KEY_PIN_HASH],
            salt = p[KEY_SALT],
            fingerprintUnlockEnabled = p[KEY_FP_UNLOCK] ?: false
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { it[KEY_DARK_THEME] = enabled }
    }

    suspend fun setLockEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_LOCK_ENABLED] = enabled }
    }

    suspend fun setPinCredentials(pinHash: String, salt: String) {
        dataStore.edit {
            it[KEY_PIN_HASH] = pinHash
            it[KEY_SALT] = salt
        }
    }

    suspend fun clearPinCredentials() {
        dataStore.edit {
            it.remove(KEY_PIN_HASH)
            it.remove(KEY_SALT)
        }
    }

    suspend fun setFingerprintUnlock(enabled: Boolean) {
        dataStore.edit { it[KEY_FP_UNLOCK] = enabled }
    }

    companion object {
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_SALT = stringPreferencesKey("salt")
        private val KEY_FP_UNLOCK = booleanPreferencesKey("fp_unlock")
    }
}
