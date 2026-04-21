package com.docvault.app.source

import android.net.Uri

/**
 * Best-effort label from the content [Uri] (picker often exposes path or authority hints).
 */
object SourceDetector {

    fun detect(uri: Uri): String {
        val full = uri.toString().lowercase()
        val auth = uri.authority?.lowercase() ?: ""
        val path = uri.path?.lowercase() ?: ""

        return when {
            "whatsapp" in full || "whatsapp" in auth || "whatsapp" in path -> "WhatsApp"
            "com.google.android.gm" in auth ||
                "gmail" in full ||
                "gmail" in path ||
                "googlemail" in full -> "Gmail"
            "com.google.android.apps.docs" in auth ||
                "com.google.android.apps.drive" in auth ||
                "drive.google" in full -> "Google Drive"
            "com.google.android.apps.photos" in auth ||
                "photos.google" in full -> "Google Photos"
            "com.android.providers.media.documents" in auth ||
                "com.android.providers.media" in auth -> "Photos / Gallery"
            "com.android.providers.downloads.documents" in auth ||
                "download" in path && "document" in full -> "Downloads"
            "com.android.externalstorage" in auth -> "Files"
            "mms" in auth || "sms" in auth -> "Messages"
            "telegram" in full || "telegram" in auth -> "Telegram"
            "instagram" in full || "instagram" in auth -> "Instagram"
            "chrome" in auth || "browser" in auth -> "Browser"
            else -> "Other"
        }
    }
}
