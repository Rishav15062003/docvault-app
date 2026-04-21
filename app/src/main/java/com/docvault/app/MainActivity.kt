package com.docvault.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docvault.app.ui.LockScreen
import com.docvault.app.ui.SettingsScreen
import com.docvault.app.ui.VaultScreen
import com.docvault.app.ui.theme.DocVaultTheme

class MainActivity : FragmentActivity() {

    private val appSettingsViewModel: AppSettingsViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractShareUris(intent).takeIf { it.isNotEmpty() }?.let { vaultViewModel.queueShareFromIntent(it) }
        enableEdgeToEdge()
        setContent {
            val vaultVm: VaultViewModel = viewModel()
            val prefs by appSettingsViewModel.preferences.collectAsState()
            val unlocked by appSettingsViewModel.sessionUnlocked.collectAsState()
            val showSettings by appSettingsViewModel.showSettings.collectAsState()

            LaunchedEffect(prefs.lockEnabled, unlocked) {
                vaultVm.tryFlushPendingShares(prefs.lockEnabled, unlocked)
            }

            DocVaultTheme(darkTheme = prefs.useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when {
                        prefs.lockEnabled && !unlocked -> LockScreen(
                            activity = this@MainActivity,
                            prefs = prefs,
                            onUnlocked = { appSettingsViewModel.unlockSession() },
                            onVerifyPin = { pin -> appSettingsViewModel.verifyPin(pin, prefs) }
                        )
                        showSettings -> SettingsScreen(
                            prefs = prefs,
                            onBack = { appSettingsViewModel.closeSettings() },
                            onDarkThemeChange = { appSettingsViewModel.setDarkTheme(it) },
                            onSavePin = { appSettingsViewModel.saveNewPin(it) },
                            onClearLock = { appSettingsViewModel.clearPinAndLock() },
                            onFingerprintChange = { appSettingsViewModel.setFingerprintUnlock(it) }
                        )
                        else -> VaultScreen(
                            vm = vaultVm,
                            onOpenSettings = { appSettingsViewModel.openSettings() },
                            onFilePickerLifecycle = { defer ->
                                appSettingsViewModel.setDeferLockClearForFilePicker(defer)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        if (!appSettingsViewModel.shouldDeferLockClearForFilePicker()) {
            appSettingsViewModel.clearUnlockSession()
            appSettingsViewModel.closeSettings()
        }
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractShareUris(intent).takeIf { it.isNotEmpty() }?.let { vaultViewModel.queueShareFromIntent(it) }
    }
}

private fun extractShareUris(intent: Intent?): List<Uri> {
    if (intent == null) return emptyList()
    return when (intent.action) {
        Intent.ACTION_SEND -> {
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?.let { listOf(it) } ?: emptyList()
        }
        Intent.ACTION_SEND_MULTIPLE -> {
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?: emptyList()
        }
        else -> emptyList()
    }
}
