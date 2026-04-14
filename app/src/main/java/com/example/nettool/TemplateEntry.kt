package com.example.nettool

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "template_table")
data class TemplateEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val rulesJson: String,
    val enabled: Boolean = true,
    val duplicateKeys: String = "[]",           // JSON 数组，去重依据字段
    val conflictStrategy: String = "{}"         // JSON 对象，冲突处理策略
) {
    companion object {
        fun fromJson(json: String): TemplateEntry {
            val root = JSONObject(json)
            val name = root.getString("name")
            val rulesArray = root.getJSONArray("rules")
            val rules = JSONArray()
            for (i in 0 until rulesArray.length()) {
                val rule = rulesArray.getJSONObject(i)
                val obj = JSONObject()
                obj.put("keyword", rule.getString("keyword"))
                obj.put("targetField", rule.getString("targetField"))
                obj.put("extractUntil", "line")
                rules.put(obj)
            }
            val duplicateKeys = root.optJSONArray("duplicateKeys")?.toString() ?: "[]"
            val conflictStrategy = root.optJSONObject("conflictStrategy")?.toString() ?: "{}"
            return TemplateEntry(
                name = name,
                rulesJson = rules.toString(),
                enabled = true,
                duplicateKeys = duplicateKeys,
                conflictStrategy = conflictStrategy
            )
        }
    }
}
