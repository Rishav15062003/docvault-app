package com.docvault.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromKind(value: ItemKind): String = value.name

    @TypeConverter
    fun toKind(value: String): ItemKind = ItemKind.valueOf(value)
}
