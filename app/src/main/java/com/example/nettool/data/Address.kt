package com.example.nettool.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "addresses")
data class Address(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ip: String,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)