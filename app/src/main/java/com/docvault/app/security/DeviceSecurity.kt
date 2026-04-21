package com.docvault.app.security

import android.content.Context
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager

fun Context.hasFingerprintSensor(): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)

fun Context.canUseFingerprintUnlock(): Boolean {
    if (!hasFingerprintSensor()) return false
    val bm = BiometricManager.from(this)
    return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS ||
        bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
}
