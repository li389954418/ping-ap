package com.example.nettool

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val allEntries = db.ipDao().getAllEntries()
    private val allTemplates = db.templateDao().getAllTemplates()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val entries = combine(allEntries, _searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.address.contains(query, ignoreCase = true)
        }
    }

    val templates = allTemplates

    private val _pingResult = MutableStateFlow("")
    val pingResult: StateFlow<String> = _pingResult

    private val _selectedAddress = MutableStateFlow("")
    val selectedAddress: StateFlow<String> = _selectedAddress

    private val _autoPingAddress = MutableStateFlow("")
    val autoPingAddress: StateFlow<String> = _autoPingAddress

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addEntry(name: String, address: String, extraRemarks: String = "{}") {
        viewModelScope.launch {
            db.ipDao().insert(IpEntry(name = name, address = address, extraRemarks = extraRemarks))
        }
    }

    fun updateEntry(entry: IpEntry) {
        viewModelScope.launch {
            db.ipDao().update(entry)
        }
    }

    fun deleteEntry(entry: IpEntry) {
        viewModelScope.launch {
            db.ipDao().delete(entry)
        }
    }

    fun addTemplate(template: TemplateEntry) {
        viewModelScope.launch {
            db.templateDao().insert(template)
        }
    }

    fun updateTemplate(template: TemplateEntry) {
        viewModelScope.launch {
            db.templateDao().update(template)
        }
    }

    fun deleteTemplate(template: TemplateEntry) {
        viewModelScope.launch {
            db.templateDao().delete(template)
        }
    }

    fun pingAddress(address: String) {
        viewModelScope.launch {
            _pingResult.value = "正在 Ping $address ...\n"
            try {
                IcmpPing.ping(address, count = 4).collect { line ->
                    _pingResult.value = _pingResult.value + line + "\n"
                }
            } catch (e: Exception) {
                _pingResult.value = _pingResult.value + "Ping 失败: ${e.message}\n"
            }
        }
    }

    fun setSelectedAddress(address: String) {
        _selectedAddress.value = address
    }

    fun triggerAutoPing(address: String) {
        _autoPingAddress.value = address
        _selectedAddress.value = address
    }

    fun clearAutoPing() {
        _autoPingAddress.value = ""
    }

    // 智能解析：根据所有启用模板提取信息
    fun autoParseAndPreview(text: String): List<IpEntry> {
        val enabledTemplates = runBlocking { db.templateDao().getEnabledTemplates().firstOrNull() ?: emptyList() }
        if (enabledTemplates.isEmpty()) return emptyList()

        val addressCandidates = mutableListOf<String>()
        val nameCandidates = mutableListOf<String>()
        val remarkMap = mutableMapOf<String, MutableList<String>>()

        for (template in enabledTemplates) {
            try {
                val rulesArray = JSONArray(template.rulesJson)
                for (i in 0 until rulesArray.length()) {
                    val rule = rulesArray.getJSONObject(i)
                    val keyword = rule.getString("keyword")
                    val targetField = rule.getString("targetField")
                    val extractUntil = rule.optString("extractUntil", "line")

                    // 在文本中查找关键词
                    val keywordIndex = text.indexOf(keyword, ignoreCase = true)
                    if (keywordIndex >= 0) {
                        // 提取内容：从关键词后开始，直到行尾或下一个关键词前
                        val startIndex = keywordIndex + keyword.length
                        val remaining = text.substring(startIndex)
                        val endIndex = when (extractUntil) {
                            "line" -> remaining.indexOfAny(charArrayOf('\n', '\r'))
                            else -> remaining.indexOf(' ')
                        }
                        val extracted = if (endIndex > 0) {
                            remaining.substring(0, endIndex).trim()
                        } else {
                            remaining.trim()
                        }

                        if (extracted.isNotEmpty()) {
                            when {
                                targetField == "address" -> {
                                    if (!addressCandidates.contains(extracted)) {
                                        addressCandidates.add(extracted)
                                    }
                                }
                                targetField == "name" -> {
                                    if (!nameCandidates.contains(extracted)) {
                                        nameCandidates.add(extracted)
                                    }
                                }
                                targetField.startsWith("remark_") -> {
                                    val key = targetField.removePrefix("remark_")
                                    remarkMap.getOrPut(key) { mutableListOf() }.add(extracted)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // 忽略解析错误
            }
        }

        // 如果没有地址，则无法保存
        if (addressCandidates.isEmpty()) return emptyList()

        val primaryAddress = addressCandidates.first()
        val primaryName = nameCandidates.firstOrNull() ?: primaryAddress

        val extraJson = JSONObject()
        // 处理多IP：第二个起存为IP2, IP3...
        addressCandidates.drop(1).forEachIndexed { index, ip ->
            extraJson.put("IP${index + 2}", ip)
        }
        // 备注字段（冲突取第一个）
        remarkMap.forEach { (key, values) ->
            extraJson.put(key, values.firstOrNull() ?: "")
        }

        return listOf(
            IpEntry(
                name = primaryName.ifBlank { "未命名" },
                address = primaryAddress,
                extraRemarks = extraJson.toString()
            )
        )
    }

    fun batchSaveEntries(entries: List<IpEntry>) {
        viewModelScope.launch {
            entries.forEach { entry ->
                db.ipDao().insert(entry)
            }
        }
    }
}