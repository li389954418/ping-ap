package com.example.nettool

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IpDao {
    @Query("SELECT * FROM ip_table ORDER BY id DESC")
    fun getAllEntries(): Flow<List<IpEntry>>

    @Insert
    suspend fun insert(entry: IpEntry)

    @Update
    suspend fun update(entry: IpEntry)

    @Delete
    suspend fun delete(entry: IpEntry)
}