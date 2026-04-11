package com.example.nettool

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM template_table ORDER BY id DESC")
    fun getAllTemplates(): Flow<List<TemplateEntry>>

    @Insert
    suspend fun insert(template: TemplateEntry)

    @Update
    suspend fun update(template: TemplateEntry)

    @Delete
    suspend fun delete(template: TemplateEntry)

    @Query("SELECT * FROM template_table WHERE enabled = 1")
    fun getEnabledTemplates(): Flow<List<TemplateEntry>>
}