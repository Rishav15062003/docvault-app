package com.docvault.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedItemDao {
    @Query("SELECT * FROM saved_items ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<SavedItem>>

    @Query("SELECT COUNT(*) FROM saved_items WHERE contentHash = :hash AND contentHash != ''")
    suspend fun countByContentHash(hash: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SavedItem)

    @Delete
    suspend fun delete(item: SavedItem)
}
