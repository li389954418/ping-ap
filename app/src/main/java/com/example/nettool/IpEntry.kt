package com.example.nettool

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ip_table")
data class IpEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val address: String,
    val extraRemarks: String = "{}",
    val category: String = "互联网",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val userName: String = ""  // 创建/编辑时的使用人
)
