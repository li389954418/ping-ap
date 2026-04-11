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

    // 模板操作
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

    // 智能解析：自动解析文本并生成预览条目
    fun autoParseAndPreview(text: String): List<IpEntry> {
        val enabledTemplates = runBlocking { db.templateDao().getEnabledTemplates().firstOrNull() ?: emptyList() }
        if (enabledTemplates.isEmpty()) return emptyList()

        val addressMatches = mutableListOf<String>()
        val nameMatches = mutableListOf<String>()
        val remarkMatches = mutableMapOf<String, MutableList<String>>()

        for (template in enabledTemplates) {
            val pattern = try {
                Regex(template.pattern)
            } catch (e: Exception) {
                continue
            }
            pattern.findAll(text).forEach { match ->
                val value = match.value
                when (template.targetField) {
                    "address" -> addressMatches.add(value)
                    "name" -> nameMatches.add(value)
                    else -> {
                        if (template.targetField.startsWith("remark_")) {
                            val key = template.targetField.removePrefix("remark_")
                            remarkMatches.getOrPut(key) { mutableListOf() }.add(value)
                        }
                    }
                }
            }
        }

        // 无 IP 地址则返回空
        if (addressMatches.isEmpty()) return emptyList()

        val primaryAddress = addressMatches.first()
        val primaryName = nameMatches.firstOrNull() ?: primaryAddress

        val extraJson = JSONObject()
        // 处理多 IP：第二个起存为 IP2, IP3...
        addressMatches.drop(1).forEachIndexed { index, ip ->
            extraJson.put("IP${index + 2}", ip)
        }
        // 备注字段（冲突取第一个）
        remarkMatches.forEach { (key, values) ->
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

    // 批量保存条目
    fun batchSaveEntries(entries: List<IpEntry>) {
        viewModelScope.launch {
            entries.forEach { entry ->
                db.ipDao().insert(entry)
            }
        }
    }
}