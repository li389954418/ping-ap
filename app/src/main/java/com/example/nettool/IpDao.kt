package com.example.nettool

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IpDao {
    @Query("SELECT * FROM ip_table ORDER BY id DESC")
    fun getAllEntries(): Flow<List<IpEntry>>

    @Query("SELECT * FROM ip_table WHERE category = :category ORDER BY id DESC")
    fun getEntriesByCategory(category: String): Flow<List<IpEntry>>

    @Query("SELECT * FROM ip_table WHERE deleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedEntries(): Flow<List<IpEntry>>

    @Query("SELECT * FROM ip_table")
    suspend fun getAllEntriesOnce(): List<IpEntry>

    @Insert
    suspend fun insert(entry: IpEntry)

    @Update
    suspend fun update(entry: IpEntry)

    @Delete
    suspend fun delete(entry: IpEntry)

    @Query("DELETE FROM ip_table WHERE deleted = 1")
    suspend fun permanentlyDeleteAllDeleted()

    @Query("DELETE FROM ip_table")
    suspend fun deleteAll()
}
