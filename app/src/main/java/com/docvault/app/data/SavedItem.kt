package com.docvault.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_items")
data class SavedItem(
    @PrimaryKey val id: String,
    val displayName: String,
    val mimeType: String,
    val storedFileName: String,
    val thumbFileName: String?,
    val kind: ItemKind,
    val addedAt: Long = System.currentTimeMillis(),
    val sourceLabel: String = "Unknown",
    val contentHash: String = ""
)

enum class ItemKind {
    IMAGE,
    PDF,
    WORD,
    OTHER
}
