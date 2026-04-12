package com.example.nettool

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category_table ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntry>>

    @Insert
    suspend fun insert(category: CategoryEntry)

    @Update
    suspend fun update(category: CategoryEntry)

    @Delete
    suspend fun delete(category: CategoryEntry)

    @Query("SELECT allowPing FROM category_table WHERE name = :name LIMIT 1")
    suspend fun isPingAllowed(name: String): Boolean
}