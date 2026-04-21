package com.docvault.app.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.docvault.app.preferences.AppPreferences
import com.docvault.app.security.hasFingerprintSensor

private const val PIN_LEN = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: AppPreferences,
    onBack: () -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onSavePin: (String) -> Unit,
    onClearLock: () -> Unit,
    onFingerprintChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val showFingerprintOption = context.hasFingerprintSensor()

    var pinSetupOpen by remember { mutableStateOf(false) }
    var pinStep by remember { mutableIntStateOf(0) }
    var firstPin by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    if (pinSetupOpen) {
        Dialog(onDismissRequest = {
            pinSetupOpen = false
            pinStep = 0
            firstPin = ""
            pinInput = ""
            pinError = false
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (pinStep == 0) "Create a 4-digit PIN" else "Confirm your PIN",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (pinError) "PINs did not match — try again" else "\u00a0",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.height(20.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(PIN_LEN) { i ->
                            val filled = i < pinInput.length
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (filled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    PinKeypad(
                        onDigit = {
                            pinError = false
                            if (pinInput.length < PIN_LEN) pinInput += it
                        },
                        onBackspace = {
                            if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                        }
                    )
                }
            }
        }
        LaunchedEffect(pinInput, pinStep) {
            if (pinInput.length != PIN_LEN) return@LaunchedEffect
            when (pinStep) {
                0 -> {
                    firstPin = pinInput
                    pinInput = ""
                    pinStep = 1
                }
                1 -> {
                    if (pinInput == firstPin) {
                        onSavePin(pinInput)
                        pinSetupOpen = false
                        pinStep = 0
                        firstPin = ""
                        pinInput = ""
                        pinError = false
                    } else {
                        pinError = true
                        pinInput = ""
                        pinStep = 0
                        firstPin = ""
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dark theme", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Black background",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = prefs.useDarkTheme,
                    onCheckedChange = onDarkThemeChange
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Security",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("App lock", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "PIN required to open the vault",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = prefs.lockEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            pinSetupOpen = true
                            pinStep = 0
                            firstPin = ""
                            pinInput = ""
                            pinError = false
                        } else {
                            onClearLock()
                        }
                    }
                )
            }

            if (showFingerprintOption) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fingerprint unlock", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Use fingerprint sensor (PIN stays available)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.fingerprintUnlockEnabled,
                        onCheckedChange = onFingerprintChange,
                        enabled = prefs.lockEnabled
                    )
                }
            }
        }
    }
}
