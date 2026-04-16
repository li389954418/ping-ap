package com.example.nettool

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SavedListScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateToHome: () -> Unit = {}
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val context = LocalContext.current

    var editingEntry by remember { mutableStateOf<IpEntry?>(null) }
    var showDetailDialog by remember { mutableStateOf<IpEntry?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showMenuForEntry by remember { mutableStateOf<IpEntry?>(null) }

    var remarkItems by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var mainRemark by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var imsPort by remember { mutableStateOf("") }
    var imsNumber by remember { mutableStateOf("") }
    var imsPassword by remember { mutableStateOf("") }

    var showImsTableExpanded by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String, label: String = "复制内容") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
    }

    fun getFullInfo(entry: IpEntry): String {
        val json = try { JSONObject(entry.extraRemarks) } catch (e: Exception) { JSONObject() }
        val addr = json.optString("地址", "").ifBlank { json.optString("address", "") }
        return buildString {
            appendLine("客户名称: ${entry.name}")
            appendLine("IP地址: ${entry.address}")
            if (addr.isNotBlank()) appendLine("地址: $addr")
            json.keys().forEach { key ->
                if (key != "地址" && key != "address" && !key.matches(Regex("IP\\d+")) && !key.startsWith("ims_")) {
                    appendLine("$key: ${json.optString(key)}")
                }
            }
        }
    }

    fun getImsGroups(extraRemarks: String): List<ImsGroup> {
        val json = try { JSONObject(extraRemarks) } catch (e: Exception) { JSONObject() }
        val groups = mutableMapOf<String, MutableMap<String, String>>()
        json.keys().forEach { key ->
            when {
                key.startsWith("ims_port_") -> {
                    val index = key.removePrefix("ims_port_")
                    groups.getOrPut(index) { mutableMapOf() }["port"] = json.optString(key)
                }
                key.startsWith("ims_number_") -> {
                    val index = key.removePrefix("ims_number_")
                    groups.getOrPut(index) { mutableMapOf() }["number"] = json.optString(key)
                }
                key.startsWith("ims_password_") -> {
                    val index = key.removePrefix("ims_password_")
                    groups.getOrPut(index) { mutableMapOf() }["password"] = json.optString(key)
                }
            }
        }
        return groups.map { (index, map) ->
            ImsGroup(
                port = map["port"] ?: "",
                number = map["number"] ?: "",
                password = map["password"] ?: ""
            )
        }.sortedBy { it.port.toIntOrNull() ?: 999 }
    }

    fun startEditing(entry: IpEntry) {
        editingEntry = entry
        mainRemark = entry.name
        val json = try { JSONObject(entry.extraRemarks) } catch (e: Exception) { JSONObject() }
        customerAddress = json.optString("地址", "").ifBlank { json.optString("address", "") }
        imsPort = json.optString("ims_port", "")
        imsNumber = json.optString("ims_number", "")
        imsPassword = json.optString("ims_password", "")
        val items = mutableListOf<Pair<String, String>>()
        json.keys().forEach { key ->
            if (key != "地址" && key != "address" && !key.matches(Regex("IP\\d+"))
                && !key.startsWith("ims_")) {
                items.add(key to json.optString(key, ""))
            }
        }
        remarkItems = items
    }

    fun getCustomerAddress(extraRemarks: String): String {
        return try {
            val json = JSONObject(extraRemarks)
            json.optString("地址", "").ifBlank { json.optString("address", "").ifBlank { "—" } }
        } catch (e: Exception) { "—" }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (categories.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = categories.indexOfFirst { it == selectedCategory }.coerceAtLeast(0),
                edgePadding = 0.dp
            ) {
                categories.forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { viewModel.setSelectedCategory(category) },
                        text = { Text(category) }
                    )
                }
                Tab(
                    selected = false,
                    onClick = { showAddCategoryDialog = true },
                    text = { Icon(Icons.Default.Add, contentDescription = "新增分页") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("🔍 搜索") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(entries, key = { it.id }) { entry ->
                val allowPing = viewModel.isCategoryAllowPing(entry.category)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .combinedClickable(
                            onClick = {
                                if (allowPing) {
                                    viewModel.triggerAutoPing(entry.address)
                                    onNavigateToHome()
                                }
                            },
                            onDoubleClick = { copyToClipboard(entry.address, "IP地址") },
                            onLongClick = { showMenuForEntry = entry }
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.name.ifBlank { "未命名" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(entry.address, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("📍 ${getCustomerAddress(entry.extraRemarks)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (selectedCategory == "全部" && entry.category != "互联网") {
                                Text("分类: ${entry.category}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(onClick = { showDetailDialog = entry }) {
                            Icon(Icons.Default.Info, contentDescription = "详情")
                        }
                    }
                }

                DropdownMenu(
                    expanded = showMenuForEntry?.id == entry.id,
                    onDismissRequest = { showMenuForEntry = null }
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            showMenuForEntry = null
                            startEditing(entry)
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            showMenuForEntry = null
                            viewModel.softDeleteEntry(entry)
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("新增分页") },
            text = { OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = { Text("分页名称") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { if (newCategoryName.isNotBlank()) viewModel.addCategory(newCategoryName); showAddCategoryDialog = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showAddCategoryDialog = false }) { Text("取消") } }
        )
    }

    // 详情弹窗
    showDetailDialog?.let { entry ->
        val extraJson = try { JSONObject(entry.extraRemarks) } catch (e: Exception) { JSONObject() }
        val items = mutableListOf<Pair<String, String>>()
        extraJson.keys().forEach { key ->
            when {
                key == "地址" || key == "address" || key.matches(Regex("IP\\d+")) -> {}
                key.startsWith("ims_") && !key.contains("_port_") && !key.contains("_number_") && !key.contains("_password_") -> {
                    items.add(key.removePrefix("ims_") to extraJson.optString(key, ""))
                }
                !key.startsWith("ims_") -> items.add(key to extraJson.optString(key, ""))
            }
        }
        val imsGroups = if (entry.category == "IMS") getImsGroups(entry.extraRemarks) else emptyList()

        AlertDialog(
            onDismissRequest = { showDetailDialog = null },
            title = { Text("详细信息") },
            text = {
                Column(modifier = Modifier.heightIn(max = 500.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("客户名称: ", fontWeight = FontWeight.Bold)
                        Text(entry.name, modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { copyToClipboard(entry.name, "客户名称") }).weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("IP/域名: ", fontWeight = FontWeight.Bold)
                        Text(entry.address, modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { copyToClipboard(entry.address, "IP地址") }).weight(1f))
                    }
                    if (entry.category != "互联网") {
                        Text("分类: ${entry.category}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val addr = extraJson.optString("地址", "").ifBlank { extraJson.optString("address", "") }
                    if (addr.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("地址: ", fontWeight = FontWeight.Bold)
                            Text(addr, modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { copyToClipboard(addr, "地址") }).weight(1f))
                        }
                    }

                    // IMS 号码表
                    if (imsGroups.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { showImsTableExpanded = !showImsTableExpanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📞 号码表 (${imsGroups.size}组)", fontWeight = FontWeight.Bold)
                                    Icon(
                                        if (showImsTableExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null
                                    )
                                }
                                if (showImsTableExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    imsGroups.forEach { group ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("端口: ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text(group.port, fontSize = 12.sp, modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { copyToClipboard(group.port, "端口") }))
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("号码: ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text(group.number, fontSize = 12.sp, modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { copyToClipboard(group.number, "号码") }))
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("密码: ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text(group.password, fontSize = 12.sp, modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { copyToClipboard(group.password, "密码") }))
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val content = "端口: ${group.port}\n号码: ${group.number}\n密码: ${group.password}"
                                                        copyToClipboard(content, "号码信息")
                                                    }
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 其他备注
                    if (items.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("额外备注", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        items.forEach { (k, v) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$k: ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(v, fontSize = 12.sp, modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { copyToClipboard(v, k) }))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { copyToClipboard(getFullInfo(entry), "全部信息") }) { Text("复制全部") }
                    TextButton(onClick = { showDetailDialog = null }) { Text("关闭") }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDetailDialog = null
                    showMenuForEntry = entry
                }) { Text("编辑") }
            }
        )
    }

    // 编辑对话框
    if (editingEntry != null) {
        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text("编辑信息") },
            text = {
                Column {
                    OutlinedTextField(value = mainRemark, onValueChange = { mainRemark = it }, label = { Text("客户名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editingEntry!!.address, onValueChange = { editingEntry = editingEntry!!.copy(address = it) }, label = { Text("IP 地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = customerAddress, onValueChange = { customerAddress = it }, label = { Text("客户地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    if (editingEntry!!.category == "IMS") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("号码信息", style = MaterialTheme.typography.titleSmall)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = imsPort, onValueChange = { imsPort = it }, label = { Text("端口", fontSize = 10.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = imsNumber, onValueChange = { imsNumber = it }, label = { Text("号码", fontSize = 10.sp) }, modifier = Modifier.weight(2f), singleLine = true)
                            OutlinedTextField(value = imsPassword, onValueChange = { imsPassword = it }, label = { Text("密码", fontSize = 10.sp) }, modifier = Modifier.weight(3f), singleLine = true)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("额外备注", style = MaterialTheme.typography.titleSmall)
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        itemsIndexed(remarkItems) { index, (key, value) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(value = key, onValueChange = { newKey -> remarkItems = remarkItems.toMutableList().apply { set(index, newKey to value) } }, label = { Text("备注名称") }, singleLine = true)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(value = value, onValueChange = { newValue -> remarkItems = remarkItems.toMutableList().apply { set(index, key to newValue) } }, label = { Text("内容") }, singleLine = true)
                                }
                                IconButton(onClick = { remarkItems = remarkItems.toMutableList().apply { removeAt(index) } }) { Icon(Icons.Default.Delete, contentDescription = "删除") }
                            }
                        }
                    }
                    TextButton(onClick = { remarkItems = remarkItems + ("" to "") }) { Icon(Icons.Default.Add, contentDescription = null); Text("新增备注") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingEntry?.let { entry ->
                        val json = JSONObject()
                        try { JSONObject(entry.extraRemarks).keys().forEach { key -> if (key.matches(Regex("IP\\d+"))) json.put(key, JSONObject(entry.extraRemarks).get(key)) } } catch (_: Exception) {}
                        if (customerAddress.isNotBlank()) json.put("地址", customerAddress)
                        if (entry.category == "IMS") {
                            if (imsPort.isNotBlank()) json.put("ims_port", imsPort)
                            if (imsNumber.isNotBlank()) json.put("ims_number", imsNumber)
                            if (imsPassword.isNotBlank()) json.put("ims_password", imsPassword)
                        }
                        remarkItems.forEach { (k, v) -> if (k.isNotBlank()) json.put(k, v) }
                        viewModel.updateEntry(entry.copy(name = mainRemark, extraRemarks = json.toString()))
                    }
                    editingEntry = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editingEntry = null }) { Text("取消") } }
        )
    }
}

data class ImsGroup(val port: String, val number: String, val password: String)
