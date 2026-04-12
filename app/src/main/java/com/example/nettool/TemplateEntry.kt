package com.example.nettool

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "template_table")
data class TemplateEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val rulesJson: String,        // JSON数组：[{"keyword":"Router","targetField":"remark_设备"},...]
    val enabled: Boolean = true
)