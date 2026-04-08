package com.example.nettool.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(address: Address)

    @Delete
    suspend fun delete(address: Address)

    @Query("SELECT * FROM addresses ORDER BY createdAt DESC")
    fun getAllAddresses(): Flow<List<Address>>

    @Query("SELECT * FROM addresses WHERE ip LIKE :keyword OR note LIKE :keyword ORDER BY createdAt DESC")
    fun searchAddresses(keyword: String): Flow<List<Address>>
}