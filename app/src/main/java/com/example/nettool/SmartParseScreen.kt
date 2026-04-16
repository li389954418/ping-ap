package com.example.nettool

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartParseScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }
    var route by remember { mutableStateOf("") }
    var imsLocator by remember { mutableStateOf("") }
    var imsRawText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    var previewEntries by remember { mutableStateOf<List<IpEntry>>(emptyList()) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }

    var editingEntry by remember { mutableStateOf<IpEntry?>(null) }
    var mainRemark by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("互联网") }

    var remarkItems by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    var duplicateData by remember { mutableStateOf<Triple<IpEntry, IpEntry, TemplateEntry>?>(null) }
    var showDuplicateDialog by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val templates by viewModel.templates.collectAsState(initial = emptyList())
    val allEntries by viewModel.entries.collectAsState(initial = emptyList())
    var categoryExpanded by remember { mutableStateOf(false) }
    var imsDropdownExpanded by remember { mutableStateOf(false) }

    val imsEntries = allEntries.filter { it.category == "IMS" }
    val filteredImsEntries = if (imsLocator.isNotBlank()) {
        imsEntries.filter {
            it.address.contains(imsLocator, ignoreCase = true) ||
            it.name.contains(imsLocator, ignoreCase = true) ||
            (try { JSONObject(it.extraRemarks).optString("产品实例标识").contains(imsLocator) } catch (e: Exception) { false })
        }
    } else emptyList()

    fun startEditing(entry: IpEntry) {
        editingEntry = entry
        mainRemark = entry.name
        val json = try { JSONObject(entry.extraRemarks) } catch (e: Exception) { JSONObject() }
        customerAddress = json.optString("地址", "").ifBlank { json.optString("address", "") }
        selectedCategory = entry.category
        val items = mutableListOf<Pair<String, String>>()
        json.keys().forEach { key ->
            when {
                key == "地址" || key == "address" || key.matches(Regex("IP\\d+")) ||
                key.startsWith("ims_") || key == "route" -> { }
                else -> items.add(key to json.optString(key, ""))
            }
        }
        remarkItems = items
    }

    fun saveCurrentEntry(closeAfter: Boolean = true) {
        editingEntry?.let { entry ->
            val json = JSONObject()
            try {
                val oldJson = JSONObject(entry.extraRemarks)
                oldJson.keys().forEach { key -> if (key.matches(Regex("IP\\d+"))) json.put(key, oldJson.get(key)) }
            } catch (_: Exception) {}
            if (customerAddress.isNotBlank()) json.put("地址", customerAddress)
            if (route.isNotBlank()) json.put("route", route)
            remarkItems.forEach { (key, value) -> if (key.isNotBlank()) json.put(key, value) }

                    val currentUserName = try { runBlocking { context.dataStore.data.first()[ThemeManager.USER_NAME] ?: "" } } catch (e: Exception) { "" }
            val updatedEntry = entry.copy(userName = currentUserName, name = mainRemark, extraRemarks = json.toString(), category = selectedCategory)

            scope.launch {
                val enabledTemplates = templates.filter { it.enabled }
                val template = enabledTemplates.firstOrNull()
                if (template != null) {
                    val duplicate = viewModel.findDuplicateEntryByTemplate(updatedEntry, template)
                    if (duplicate != null) {
                        duplicateData = Triple(duplicate, updatedEntry, template)
                        showDuplicateDialog = true
                        return@launch
                    }
                }
                viewModel.batchSaveEntries(listOf(updatedEntry))
                if (closeAfter) { showConfirmDialog = false; showQuickAddDialog = false; onBack() }
                else { previewEntries = emptyList(); editingEntry = null; showConfirmDialog = false; showQuickAddDialog = false }
            }
        }
    }

    fun startQuickAdd() {
        editingEntry = IpEntry(name = "", address = "", extraRemarks = "{}", category = "互联网")
        mainRemark = ""; customerAddress = ""; selectedCategory = "互联网"; remarkItems = emptyList()
        showQuickAddDialog = true
    }

    fun handleRecognize() {
        if (imsRawText.isBlank()) { Toast.makeText(context, "请输入原始文本", Toast.LENGTH_SHORT).show(); return }
        scope.launch {
            val lines = imsRawText.lines().filter { it.isNotBlank() }
            val isBatch = lines.size > 1 || (lines.size == 1 && lines[0].trim().split(Regex("\\s+")).size >= 2)
            if (isBatch) {
                var success = 0; var fail = 0
                for (line in lines) {
                    val parts = line.trim().split(Regex("\\s+"), limit = 2)
                    if (parts.size < 2) { fail++; continue }
                    val locator = parts[0]; val raw = parts[1]
                    val target = viewModel.findImsEntry(locator)
                    if (target == null) { fail++; continue }
                    val (port, number, password) = viewModel.parseImsInfo(raw)
                    if (port.isNotBlank() || number.isNotBlank() || password.isNotBlank()) {
                        viewModel.updateImsEntry(target, port, number, password)
                        success++
                    } else { fail++ }
                }
                Toast.makeText(context, "批量完成: 成功 $success 条, 失败 $fail 条", Toast.LENGTH_LONG).show()
            } else {
                if (imsLocator.isBlank()) { Toast.makeText(context, "请选择或输入定位信息", Toast.LENGTH_SHORT).show(); return@launch }
                val target = viewModel.findImsEntry(imsLocator)
                if (target == null) { Toast.makeText(context, "未找到匹配的 IMS 记录", Toast.LENGTH_SHORT).show(); return@launch }
                val (port, number, password) = viewModel.parseImsInfo(imsRawText)
                if (port.isNotBlank() || number.isNotBlank() || password.isNotBlank()) {
                    viewModel.updateImsEntry(target, port, number, password)
                    Toast.makeText(context, "已更新 IMS 记录: ${target.name}", Toast.LENGTH_SHORT).show()
                    imsLocator = ""
                } else {
                    Toast.makeText(context, "未能识别到有效信息", Toast.LENGTH_SHORT).show()
                }
            }
            imsRawText = ""
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("智能解析", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = inputText, onValueChange = { inputText = it }, label = { Text("粘贴文档内容") }, modifier = Modifier.fillMaxWidth().height(150.dp), maxLines = 8)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = route, onValueChange = { route = it }, label = { Text("路由（非必填）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                isProcessing = true
                previewEntries = viewModel.autoParseAndPreview(inputText, selectedCategory)
                isProcessing = false
                if (previewEntries.isNotEmpty()) { startEditing(previewEntries.first()); showConfirmDialog = true }
                else { Toast.makeText(context, "未提取到有效信息", Toast.LENGTH_SHORT).show() }
            }, modifier = Modifier.weight(1f), enabled = inputText.isNotBlank() && !isProcessing) {
                if (isProcessing) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) }
                else { Text("智能解析") }
            }
            Button(onClick = { startQuickAdd() }, modifier = Modifier.weight(1f)) { Text("快速添加") }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("📞 号码保存", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = imsDropdownExpanded && filteredImsEntries.isNotEmpty(),
            onExpandedChange = { if (imsLocator.isNotBlank()) imsDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = imsLocator,
                onValueChange = { imsLocator = it; imsDropdownExpanded = it.isNotBlank() },
                label = { Text("搜索 IMS 记录（IP/名称/产品标识）") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                singleLine = true
            )
            if (filteredImsEntries.isNotEmpty()) {
                ExposedDropdownMenu(expanded = imsDropdownExpanded, onDismissRequest = { imsDropdownExpanded = false }) {
                    filteredImsEntries.take(10).forEach { entry ->
                        DropdownMenuItem(
                            text = { Column { Text(entry.name.ifBlank { "未命名" }); Text(entry.address, style = MaterialTheme.typography.bodySmall) } },
                            onClick = { imsLocator = entry.address; imsDropdownExpanded = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = imsRawText, onValueChange = { imsRawText = it }, label = { Text("粘贴文本（单条或批量）") }, modifier = Modifier.fillMaxWidth(), maxLines = 5)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { handleRecognize() }, modifier = Modifier.fillMaxWidth()) { Text("🔍 识别并更新") }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showConfirmDialog && editingEntry != null) {
        EditEntryDialog(entry = editingEntry!!, mainRemark = mainRemark, onMainRemarkChange = { mainRemark = it }, customerAddress = customerAddress, onCustomerAddressChange = { customerAddress = it }, selectedCategory = selectedCategory, onCategoryChange = { selectedCategory = it }, categories = categories, remarkItems = remarkItems, onRemarkItemsChange = { remarkItems = it }, onSave = { updatedEntry -> editingEntry = updatedEntry; saveCurrentEntry() }, onDismiss = { showConfirmDialog = false })
    }
    if (showQuickAddDialog && editingEntry != null) {
        EditEntryDialog(entry = editingEntry!!, mainRemark = mainRemark, onMainRemarkChange = { mainRemark = it }, customerAddress = customerAddress, onCustomerAddressChange = { customerAddress = it }, selectedCategory = selectedCategory, onCategoryChange = { selectedCategory = it }, categories = categories, remarkItems = remarkItems, onRemarkItemsChange = { remarkItems = it }, onSave = { updatedEntry -> editingEntry = updatedEntry; saveCurrentEntry() }, onDismiss = { showQuickAddDialog = false })
    }
    if (showDuplicateDialog && duplicateData != null) {
        val (oldEntry, newEntry, template) = duplicateData!!
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false }, title = { Text("发现重复记录") },
            text = { Column { Text("根据模板 [${template.name}] 的去重规则，检测到重复。"); Spacer(modifier = Modifier.height(8.dp)); Text("现有记录：${oldEntry.name} (${oldEntry.address})"); Text("新记录：${newEntry.name.ifBlank { "未命名" }} (${newEntry.address})") } },
            confirmButton = {
                Row {
                    TextButton(onClick = { val merged = viewModel.mergeEntriesByTemplate(oldEntry, newEntry, template); viewModel.updateEntry(merged); showDuplicateDialog = false; showConfirmDialog = false; showQuickAddDialog = false; onBack(); Toast.makeText(context, "已按策略合并", Toast.LENGTH_SHORT).show() }) { Text("按策略合并") }
                    TextButton(onClick = { viewModel.updateEntry(newEntry.copy(id = oldEntry.id)); showDuplicateDialog = false; showConfirmDialog = false; showQuickAddDialog = false; onBack(); Toast.makeText(context, "已替换", Toast.LENGTH_SHORT).show() }) { Text("替换") }
                }
            },
            dismissButton = { TextButton(onClick = { showDuplicateDialog = false }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryDialog(
    entry: IpEntry, mainRemark: String, onMainRemarkChange: (String) -> Unit,
    customerAddress: String, onCustomerAddressChange: (String) -> Unit,
    selectedCategory: String, onCategoryChange: (String) -> Unit,
    categories: List<String>, remarkItems: List<Pair<String, String>>,
    onRemarkItemsChange: (List<Pair<String, String>>) -> Unit,
    onSave: (IpEntry) -> Unit, onDismiss: () -> Unit
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var currentAddress by remember { mutableStateOf(entry.address) }
    var currentName by remember { mutableStateOf(mainRemark) }
    var currentCustomerAddress by remember { mutableStateOf(customerAddress) }

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("编辑信息") },
        text = {
            Column {
                OutlinedTextField(value = currentName, onValueChange = { currentName = it; onMainRemarkChange(it) }, label = { Text("客户名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = currentAddress, onValueChange = { currentAddress = it }, label = { Text("IP 地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = currentCustomerAddress, onValueChange = { currentCustomerAddress = it; onCustomerAddressChange(it) }, label = { Text("客户地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                    OutlinedTextField(value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("分类") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        categories.forEach { category -> DropdownMenuItem(text = { Text(category) }, onClick = { onCategoryChange(category); categoryExpanded = false }) }
                    }
                }
                if (remarkItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp)); Text("额外备注", style = MaterialTheme.typography.titleSmall); Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        itemsIndexed(remarkItems) { index, (key, value) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(value = key, onValueChange = { newKey -> onRemarkItemsChange(remarkItems.toMutableList().apply { set(index, newKey to value) }) }, label = { Text("字段") }, singleLine = true)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(value = value, onValueChange = { newValue -> onRemarkItemsChange(remarkItems.toMutableList().apply { set(index, key to newValue) }) }, label = { Text("值") }, singleLine = true)
                                }
                                IconButton(onClick = { onRemarkItemsChange(remarkItems.toMutableList().apply { removeAt(index) }) }) { Icon(Icons.Default.Delete, contentDescription = "删除") }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { onRemarkItemsChange(remarkItems + ("" to "")) }) { Icon(Icons.Default.Add, contentDescription = null); Text("新增备注") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(entry.copy(name = currentName, address = currentAddress)) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
