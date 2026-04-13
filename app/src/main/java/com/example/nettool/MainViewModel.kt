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

    private val fixedCategories = listOf("全部", "互联网", "医保/供水专线", "IMS", "数据专线")
    val categories: StateFlow<List<String>> = MutableStateFlow(fixedCategories)

    private val _selectedCategory = MutableStateFlow("全部")
    val selectedCategory: StateFlow<String> = _selectedCategory

    val entries = combine(allEntries, _searchQuery, _selectedCategory) { list, query, category ->
        val filtered = when (category) {
            "全部" -> list
            else -> list.filter { it.category == category }
        }
        if (query.isBlank()) filtered
        else filtered.filter { entry ->
            entry.name.contains(query, ignoreCase = true) ||
            entry.address.contains(query, ignoreCase = true) ||
            (try {
                val json = JSONObject(entry.extraRemarks)
                json.keys().asSequence().any { key ->
                    json.optString(key).contains(query, ignoreCase = true)
                }
            } catch (e: Exception) { false })
        }
    }

    val templates = allTemplates

    private val _pingResult = MutableStateFlow("")
    val pingResult: StateFlow<String> = _pingResult

    private val _selectedAddress = MutableStateFlow("")
    val selectedAddress: StateFlow<String> = _selectedAddress

    private val _autoPingAddress = MutableStateFlow("")
    val autoPingAddress: StateFlow<String> = _autoPingAddress

    private val _editingTargetId = MutableStateFlow<Int?>(null)
    val editingTargetId: StateFlow<Int?> = _editingTargetId

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedCategory(category: String) { _selectedCategory.value = category }

    fun addEntry(name: String, address: String, extraRemarks: String = "{}", category: String = "互联网") {
        viewModelScope.launch {
            db.ipDao().insert(IpEntry(name = name, address = address, extraRemarks = extraRemarks, category = category))
        }
    }

    fun updateEntry(entry: IpEntry) {
        viewModelScope.launch { db.ipDao().update(entry) }
    }

    fun deleteEntry(entry: IpEntry) {
        viewModelScope.launch { db.ipDao().delete(entry) }
    }

    fun addTemplate(template: TemplateEntry) {
        viewModelScope.launch { db.templateDao().insert(template) }
    }

    fun updateTemplate(template: TemplateEntry) {
        viewModelScope.launch { db.templateDao().update(template) }
    }

    fun deleteTemplate(template: TemplateEntry) {
        viewModelScope.launch { db.templateDao().delete(template) }
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

    fun setSelectedAddress(address: String) { _selectedAddress.value = address }
    fun triggerAutoPing(address: String) {
        _autoPingAddress.value = address
        _selectedAddress.value = address
    }
    fun clearAutoPing() { _autoPingAddress.value = "" }

    fun requestEditEntry(id: Int) { _editingTargetId.value = id }
    fun clearEditingTarget() { _editingTargetId.value = null }

    fun autoClassify(address: String): String {
        return when {
            address.isBlank() -> "医保/供水专线"
            address.startsWith("172.") -> "IMS"
            else -> "互联网"
        }
    }

    fun autoParseAndPreview(text: String, targetCategory: String = "互联网"): List<IpEntry> {
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

                    var searchStart = 0
                    while (searchStart < text.length) {
                        val keywordIndex = text.indexOf(keyword, searchStart, ignoreCase = true)
                        if (keywordIndex < 0) break
                        val startIndex = keywordIndex + keyword.length
                        val remaining = text.substring(startIndex)
                        val endIndex = remaining.indexOfAny(charArrayOf('\n', '\r')).takeIf { it >= 0 } ?: remaining.length
                        val extracted = remaining.substring(0, endIndex).trim()
                        searchStart = startIndex + endIndex

                        if (extracted.isNotEmpty()) {
                            when {
                                targetField == "address" -> {
                                    val ips = extracted.split(Regex("[,，、\\s]+")).filter { it.isNotBlank() }
                                    ips.forEach { ip -> if (!addressCandidates.contains(ip)) addressCandidates.add(ip) }
                                }
                                targetField == "name" -> {
                                    if (!nameCandidates.contains(extracted)) nameCandidates.add(extracted)
                                }
                                targetField.startsWith("remark_") -> {
                                    val key = targetField.removePrefix("remark_")
                                    remarkMap.getOrPut(key) { mutableListOf() }.add(extracted)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) { /* ignore */ }
        }

        if (addressCandidates.isEmpty()) return emptyList()
        val primaryAddress = addressCandidates.first()
        val primaryName = nameCandidates.firstOrNull() ?: primaryAddress
        val finalCategory = if (targetCategory == "互联网") autoClassify(primaryAddress) else targetCategory

        val extraJson = JSONObject()
        addressCandidates.drop(1).forEachIndexed { index, ip -> extraJson.put("IP${index + 2}", ip) }
        remarkMap.forEach { (key, values) -> extraJson.put(key, values.distinct().joinToString(", ")) }

        return listOf(IpEntry(name = primaryName.ifBlank { "未命名" }, address = primaryAddress, extraRemarks = extraJson.toString(), category = finalCategory))
    }

    fun batchSaveEntries(entries: List<IpEntry>) {
        viewModelScope.launch { entries.forEach { db.ipDao().insert(it) } }
    }

    suspend fun findImsEntry(keyword: String): IpEntry? {
        val all = db.ipDao().getAllEntriesOnce()
        return all.find { entry ->
            entry.category == "IMS" && (entry.address.equals(keyword, ignoreCase = true) ||
                (try { JSONObject(entry.extraRemarks).optString("产品实例标识") == keyword } catch (e: Exception) { false }))
        }
    }

    fun updateImsEntry(entry: IpEntry, port: String, number: String, password: String) {
        viewModelScope.launch {
            val json = try { JSONObject(entry.extraRemarks) } catch (e: Exception) { JSONObject() }
            if (port.isNotBlank()) json.put("ims_port", port)
            if (number.isNotBlank()) json.put("ims_number", number)
            if (password.isNotBlank()) json.put("ims_password", password)
            val updated = entry.copy(extraRemarks = json.toString())
            db.ipDao().update(updated)
        }
    }

    fun parseImsInfo(text: String): Triple<String, String, String> {
        var port = ""; var number = ""; var password = ""
        Regex("\\b([1-9][0-9]{0,2})\\b").find(text)?.groupValues?.get(1)?.let { port = it }
        Regex("[\\S&&[^\\u4e00-\\u9fa5]]{6,}").find(text)?.value?.let { password = it }
        val longNumber = Regex("\\b(\\d{11})\\b").find(text)?.groupValues?.get(1)
        val shortNumber = Regex("\\b(\\d{8})\\b").find(text)?.groupValues?.get(1)
        number = longNumber ?: shortNumber ?: ""
        return Triple(port, number, password)
    }

    suspend fun findDuplicateEntry(address: String, productId: String? = null): IpEntry? {
        val all = db.ipDao().getAllEntriesOnce()
        return all.find { entry ->
            entry.address.equals(address, ignoreCase = true) ||
            (productId != null && try { JSONObject(entry.extraRemarks).optString("产品实例标识") == productId } catch (e: Exception) { false })
        }
    }

    fun mergeRemarks(oldEntry: IpEntry, newEntry: IpEntry): IpEntry {
        val oldJson = try { JSONObject(oldEntry.extraRemarks) } catch (e: Exception) { JSONObject() }
        val newJson = try { JSONObject(newEntry.extraRemarks) } catch (e: Exception) { JSONObject() }
        newJson.keys().forEach { key -> oldJson.put(key, newJson.get(key)) }
        return oldEntry.copy(name = newEntry.name.ifBlank { oldEntry.name }, address = newEntry.address.ifBlank { oldEntry.address }, extraRemarks = oldJson.toString(), category = newEntry.category)
    }
}
