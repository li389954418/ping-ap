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
    val category: String = "默认"
)