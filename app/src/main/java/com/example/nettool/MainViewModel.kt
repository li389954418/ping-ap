package com.example.nettool

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val allEntries = db.ipDao().getAllEntries()
    private val allTemplates = db.templateDao().getAllTemplates()
    private val deletedEntries = db.ipDao().getDeletedEntries()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // 动态分页列表
    private val _categories = MutableStateFlow(listOf("互联网", "供水", "医保", "IMS", "数据专线"))
    val categories: StateFlow<List<String>> = _categories

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
    val recycleBinEntries = deletedEntries

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

    fun addEntry(name: String, address: String, extraRemarks: String = "{}", category: String = "互联网", userName: String = "") {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            db.ipDao().insert(
                IpEntry(
                    name = name,
                    address = address,
                    extraRemarks = extraRemarks,
                    category = category,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun updateEntry(entry: IpEntry) {
        viewModelScope.launch {
            db.ipDao().update(entry.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun softDeleteEntry(entry: IpEntry) {
        viewModelScope.launch {
            db.ipDao().update(entry.copy(deleted = true, updatedAt = System.currentTimeMillis()))
        }
    }

    fun restoreEntry(entry: IpEntry) {
        viewModelScope.launch {
            db.ipDao().update(entry.copy(deleted = false, updatedAt = System.currentTimeMillis()))
        }
    }

    fun permanentlyDeleteEntry(entry: IpEntry) {
        viewModelScope.launch {
            db.ipDao().delete(entry)
        }
    }

    fun permanentlyDeleteAllDeleted() {
        viewModelScope.launch {
            db.ipDao().permanentlyDeleteAllDeleted()
        }
    }

    // 分页管理
    fun addCategory(category: String) {
        val current = _categories.value.toMutableList()
        if (!current.contains(category)) {
            current.add(category)
            _categories.value = current
        }
    }

    fun renameCategory(oldName: String, newName: String) {
        val current = _categories.value.toMutableList()
        val index = current.indexOf(oldName)
        if (index >= 0 && !current.contains(newName)) {
            current[index] = newName
            _categories.value = current
            // 更新所有属于该分类的条目
            viewModelScope.launch {
                val all = db.ipDao().getAllEntriesOnce()
                all.filter { it.category == oldName }.forEach { entry ->
                    db.ipDao().update(entry.copy(category = newName, updatedAt = System.currentTimeMillis()))
                }
            }
        }
    }

    fun deleteCategory(category: String) {
        val current = _categories.value.toMutableList()
        if (current.remove(category)) {
            _categories.value = current
            if (_selectedCategory.value == category) {
                _selectedCategory.value = "全部"
            }
        }
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
            "供水", "医保", "IMS", "数据专线" -> false
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
            address.isBlank() -> "供水"
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

                    val pattern = Regex("${Regex.escape(keyword)}\\s*([^\\n\\r]*)")
                    pattern.findAll(text).forEach { match ->
                        val extracted = match.groupValues[1].trim()
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

        val now = System.currentTimeMillis()
        return listOf(
            IpEntry(
                name = primaryName.ifBlank { "未命名" },
                address = primaryAddress,
                extraRemarks = extraJson.toString(),
                category = finalCategory,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    fun batchSaveEntries(entries: List<IpEntry>) {
        viewModelScope.launch { entries.forEach { db.ipDao().insert(it) } }
    }

    suspend fun findImsEntry(keyword: String): IpEntry? {
        val all = db.ipDao().getAllEntriesOnce().filter { !it.deleted }
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
            val updated = entry.copy(extraRemarks = json.toString(), updatedAt = System.currentTimeMillis())
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

    // 根据模板配置检测重复
    suspend fun findDuplicateEntryByTemplate(
        newEntry: IpEntry,
        template: TemplateEntry
    ): IpEntry? {
        val all = db.ipDao().getAllEntriesOnce().filter { !it.deleted }
        val duplicateKeys = try {
            JSONArray(template.duplicateKeys)
        } catch (e: Exception) {
            JSONArray()
        }
        if (duplicateKeys.length() == 0) return null

        val newJson = try { JSONObject(newEntry.extraRemarks) } catch (e: Exception) { JSONObject() }

        return all.find { existing ->
            duplicateKeys.let { keys ->
                for (i in 0 until keys.length()) {
                    val key = keys.getString(i)
                    val match = when (key) {
                        "address" -> newEntry.address.equals(existing.address, ignoreCase = true)
                        "name" -> newEntry.name.equals(existing.name, ignoreCase = true)
                        else -> {
                            val newValue = newJson.optString(key)
                            val existingJson = try { JSONObject(existing.extraRemarks) } catch (e: Exception) { JSONObject() }
                            val existingValue = existingJson.optString(key)
                            newValue.isNotBlank() && newValue == existingValue
                        }
                    }
                    if (match) return@let true
                }
                false
            }
        }
    }

    // 根据模板策略合并条目
    fun mergeEntriesByTemplate(oldEntry: IpEntry, newEntry: IpEntry, template: TemplateEntry): IpEntry {
        val conflictStrategy = try {
            JSONObject(template.conflictStrategy)
        } catch (e: Exception) {
            JSONObject()
        }
        val replaceFields = conflictStrategy.optJSONArray("replace") ?: JSONArray()
        val keepFields = conflictStrategy.optJSONArray("keep") ?: JSONArray()
        val mergeFields = conflictStrategy.optJSONArray("merge") ?: JSONArray()

        val oldJson = try { JSONObject(oldEntry.extraRemarks) } catch (e: Exception) { JSONObject() }
        val newJson = try { JSONObject(newEntry.extraRemarks) } catch (e: Exception) { JSONObject() }

        // 处理 replace 字段
        for (i in 0 until replaceFields.length()) {
            val field = replaceFields.getString(i)
            when (field) {
                "name" -> { /* handled separately */ }
                "address" -> { /* handled separately */ }
                else -> {
                    if (newJson.has(field)) {
                        oldJson.put(field, newJson.get(field))
                    }
                }
            }
        }

        // 处理 merge 字段
        for (i in 0 until mergeFields.length()) {
            val field = mergeFields.getString(i)
            if (newJson.has(field)) {
                val oldValue = oldJson.optString(field, "")
                val newValue = newJson.optString(field, "")
                oldJson.put(field, if (oldValue.isNotBlank()) "$oldValue, $newValue" else newValue)
            }
        }

        // keep 字段不做处理，保留旧值

        return oldEntry.copy(
            extraRemarks = oldJson.toString(),
            category = newEntry.category,
            updatedAt = System.currentTimeMillis()
        )
    }

    // 导出所有数据（条目、模板、设置）
    suspend fun exportAllData(): String {
        val allEntries = db.ipDao().getAllEntriesOnce()
        val allTemplates = db.templateDao().getAllTemplatesOnce()
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportTime", System.currentTimeMillis())

        val entriesArray = JSONArray()
        allEntries.forEach { entry ->
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("name", entry.name)
            obj.put("address", entry.address)
            obj.put("extraRemarks", entry.extraRemarks)
            obj.put("category", entry.category)
            obj.put("createdAt", entry.createdAt)
            obj.put("updatedAt", entry.updatedAt)
            obj.put("deleted", entry.deleted)
            entriesArray.put(obj)
        }
        root.put("entries", entriesArray)

        val templatesArray = JSONArray()
        allTemplates.forEach { template ->
            val obj = JSONObject()
            obj.put("id", template.id)
            obj.put("name", template.name)
            obj.put("rulesJson", template.rulesJson)
            obj.put("enabled", template.enabled)
            obj.put("duplicateKeys", template.duplicateKeys)
            obj.put("conflictStrategy", template.conflictStrategy)
            templatesArray.put(obj)
        }
        root.put("templates", templatesArray)

        return root.toString()
    }

    // 导入数据
    suspend fun importData(jsonString: String, importEntries: Boolean, importTemplates: Boolean) {
        val root = JSONObject(jsonString)
        if (importEntries && root.has("entries")) {
            val entriesArray = root.getJSONArray("entries")
            for (i in 0 until entriesArray.length()) {
                val obj = entriesArray.getJSONObject(i)
                val entry = IpEntry(
                    name = obj.optString("name"),
                    address = obj.optString("address"),
                    extraRemarks = obj.optString("extraRemarks", "{}"),
                    category = obj.optString("category", "互联网"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    deleted = obj.optBoolean("deleted", false)
                )
                db.ipDao().insert(entry)
            }
        }
        if (importTemplates && root.has("templates")) {
            val templatesArray = root.getJSONArray("templates")
            for (i in 0 until templatesArray.length()) {
                val obj = templatesArray.getJSONObject(i)
                val template = TemplateEntry(
                    name = obj.getString("name"),
                    rulesJson = obj.getString("rulesJson"),
                    enabled = obj.optBoolean("enabled", true),
                    duplicateKeys = obj.optString("duplicateKeys", "[]"),
                    conflictStrategy = obj.optString("conflictStrategy", "{}")
                )
                db.templateDao().insert(template)
            }
        }
    }
}

    suspend fun getAllEntriesSync(): List<IpEntry> {
        return db.ipDao().getAllEntriesOnce().filter { !it.deleted }
    }

    suspend fun replaceAllEntries(entries: List<IpEntry>) {
        db.ipDao().deleteAll()
        entries.forEach { db.ipDao().insert(it) }
    }

    fun exportEntries(entries: List<IpEntry>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportTime", System.currentTimeMillis())
        val entriesArray = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("name", entry.name)
            obj.put("address", entry.address)
            obj.put("extraRemarks", entry.extraRemarks)
            obj.put("category", entry.category)
            obj.put("createdAt", entry.createdAt)
            obj.put("updatedAt", entry.updatedAt)
            obj.put("deleted", entry.deleted)
            obj.put("userName", entry.userName)
            entriesArray.put(obj)
        }
        root.put("entries", entriesArray)
        return root.toString()
    suspend fun getAllEntriesSync(): List<IpEntry> {
        return db.ipDao().getAllEntriesOnce().filter { !it.deleted }
    }

    suspend fun replaceAllEntries(entries: List<IpEntry>) {
        db.ipDao().deleteAll()
        entries.forEach { entry -> db.ipDao().insert(entry) }
    }

    fun exportEntries(entries: List<IpEntry>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportTime", System.currentTimeMillis())
        val entriesArray = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("name", entry.name)
            obj.put("address", entry.address)
            obj.put("extraRemarks", entry.extraRemarks)
            obj.put("category", entry.category)
            obj.put("createdAt", entry.createdAt)
            obj.put("updatedAt", entry.updatedAt)
            obj.put("deleted", entry.deleted)
            obj.put("userName", entry.userName)
            entriesArray.put(obj)
        }
        root.put("entries", entriesArray)
        return root.toString()
    }
    }
