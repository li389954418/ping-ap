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

    private val fixedCategories = listOf("全部", "默认", "医保/供水专线", "IMS", "数据专线")
    val categories: StateFlow<List<String>> = MutableStateFlow(fixedCategories)

    private val _selectedCategory = MutableStateFlow("全部")
    val selectedCategory: StateFlow<String> = _selectedCategory

    val entries = combine(allEntries, _searchQuery, _selectedCategory) { list, query, category ->
        val filtered = when (category) {
            "全部" -> list
            else -> list.filter { it.category == category }
        }
        if (query.isBlank()) filtered
        else filtered.filter {
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

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addEntry(name: String, address: String, extraRemarks: String = "{}", category: String = "默认") {
        viewModelScope.launch {
            db.ipDao().insert(IpEntry(name = name, address = address, extraRemarks = extraRemarks, category = category))
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

    fun isCategoryAllowPing(category: String): Boolean {
        return when (category) {
            "医保/供水专线", "IMS", "数据专线" -> false
            else -> true
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

    fun autoParseAndPreview(text: String, targetCategory: String = "默认"): List<IpEntry> {
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

                    val keywordIndex = text.indexOf(keyword, ignoreCase = true)
                    if (keywordIndex >= 0) {
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
                                    val ips = extracted.split(Regex("[,，、\\s]+")).filter { it.isNotBlank() }
                                    ips.forEach { ip ->
                                        if (!addressCandidates.contains(ip)) {
                                            addressCandidates.add(ip)
                                        }
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
                // ignore
            }
        }

        if (addressCandidates.isEmpty()) return emptyList()

        val primaryAddress = addressCandidates.first()
        val primaryName = nameCandidates.firstOrNull() ?: primaryAddress

        val extraJson = JSONObject()
        addressCandidates.drop(1).forEachIndexed { index, ip ->
            extraJson.put("IP${index + 2}", ip)
        }
        remarkMap.forEach { (key, values) ->
            extraJson.put(key, values.firstOrNull() ?: "")
        }

        return listOf(
            IpEntry(
                name = primaryName.ifBlank { "未命名" },
                address = primaryAddress,
                extraRemarks = extraJson.toString(),
                category = targetCategory
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
