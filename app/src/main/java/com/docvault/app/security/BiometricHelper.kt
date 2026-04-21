package com.docvault.app.security

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun showBiometricUnlockPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onUsePin: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED
                ) {
                    onUsePin()
                }
            }

            override fun onAuthenticationFailed() {}
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Doc Vault")
        .setSubtitle("Confirm your fingerprint")
        .setNegativeButtonText("Use PIN")
        .build()
    prompt.authenticate(info)
}
