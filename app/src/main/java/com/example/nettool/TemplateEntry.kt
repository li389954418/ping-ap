package com.example.nettool

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "template_table")
data class TemplateEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,                // 模板名称
    val pattern: String,             // 正则表达式
    val targetField: String,         // 目标字段（address, name, remark_xxx）
    val enabled: Boolean = true      // 是否启用
)