package com.docvault.app.security

import java.security.MessageDigest

object PinHasher {
    fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { b -> "%02x".format(b) }
    }

    fun verify(pin: String, salt: String, expectedHash: String): Boolean =
        hash(pin, salt) == expectedHash
}
