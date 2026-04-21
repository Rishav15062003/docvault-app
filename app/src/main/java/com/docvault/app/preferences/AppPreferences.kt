package com.docvault.app.preferences

data class AppPreferences(
    val useDarkTheme: Boolean = false,
    val lockEnabled: Boolean = false,
    val pinHash: String? = null,
    val salt: String? = null,
    val fingerprintUnlockEnabled: Boolean = false
) {
    val hasPinConfigured: Boolean get() = !pinHash.isNullOrEmpty() && !salt.isNullOrEmpty()
}
