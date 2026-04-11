package com.example.nettool

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ip_table")
data class IpEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,                // 主备注名称
    val address: String,             // IP或域名
    val extraRemarks: String = "{}"  // 额外备注字段，JSON格式存储键值对
)
