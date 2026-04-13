package com.example.nettool

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedListScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateToHome: () -> Unit = {}
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var editingEntry by remember { mutableStateOf<IpEntry?>(null) }
    var showDetailDialog by remember { mutableStateOf<IpEntry?>(null) }

    var remarkItems by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var mainRemark by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var imsPort by remember { mutableStateOf("") }
    var imsNumber by remember { mutableStateOf("") }
    var imsPassword by remember { mutableStateOf("") }

    var menuExpanded by remember { mutableStateOf(false) }
    var selectedEntryForMenu by remember { mutableStateOf<IpEntry?>(null) }

    fun startEditing(entry: IpEntry) {
        editingEntry = entry
        mainRemark = entry.name
        val json = try {
            JSONObject(entry.extraRemarks)
        } catch (e: Exception) {
            JSONObject()
        }
        customerAddress = json.optString("地址", "").ifBlank { json.optString("address", "") }
        imsPort = json.optString("ims_port", "")
        imsNumber = json.optString("ims_number", "")
        imsPassword = json.optString("ims_password", "")
        val items = mutableListOf<Pair<String, String>>()
        json.keys().forEach { key ->
            if (key != "地址" && key != "address" && !key.matches(Regex("IP\\d+"))
                && key != "ims_port" && key != "ims_number" && key != "ims_password") {
                items.add(key to json.optString(key, ""))
            }
        }
        remarkItems = items
    }

    fun getCustomerAddress(extraRemarks: String): String {
        return try {
            val json = JSONObject(extraRemarks)
            json.optString("地址", "").ifBlank { json.optString("address", "").ifBlank { "—" } }
        } catch (e: Exception) {
            "—"
        }
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
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("🔍 搜索备注或IP") },
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
                        .pointerInput(entry.id) {
                            detectTapGestures(
                                onTap = {
                                    if (allowPing) {
                                        viewModel.triggerAutoPing(entry.address)
                                        onNavigateToHome()
                                    }
                                },
                                onLongPress = {
                                    selectedEntryForMenu = entry
                                    menuExpanded = true
                                }
                            )
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.name.ifBlank { "未命名" },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.address,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📍 ${getCustomerAddress(entry.extraRemarks)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (entry.category != "默认" && entry.category != "全部") {
                                Text(
                                    text = "分类: ${entry.category}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(
                            onClick = { showDetailDialog = entry }
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "查看详情",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = menuExpanded && selectedEntryForMenu?.id == entry.id,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = {
                                menuExpanded = false
                                viewModel.deleteEntry(entry)
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = {
                                menuExpanded = false
                                startEditing(entry)
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }

    // 详情弹窗
    showDetailDialog?.let { entry ->
        val extraJson = try {
            JSONObject(entry.extraRemarks)
        } catch (e: Exception) {
            JSONObject()
        }
        val items = mutableListOf<Pair<String, String>>()
        extraJson.keys().forEach { key ->
            if (key != "地址" && key != "address" && !key.matches(Regex("IP\\d+"))
                && key != "ims_port" && key != "ims_number" && key != "ims_password") {
                items.add(key to extraJson.getString(key))
            }
        }

        AlertDialog(
            onDismissRequest = { showDetailDialog = null },
            title = { /* 无标题 */ },
            text = {
                Column {
                    SelectionContainer {
                        Column {
                            Text("客户名称: ${entry.name}")
                            Text("IP/域名: ${entry.address}")
                            if (entry.category != "默认") {
                                Text("分类: ${entry.category}")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val addr = extraJson.optString("地址", "").ifBlank { extraJson.optString("address", "") }
                            if (addr.isNotBlank()) {
                                Text("地址: $addr")
                            }
                            if (entry.category == "IMS") {
                                val port = extraJson.optString("ims_port", "")
                                val number = extraJson.optString("ims_number", "")
                                val password = extraJson.optString("ims_password", "")
                                if (port.isNotBlank()) Text("端口号: $port")
                                if (number.isNotBlank()) Text("号码: $number")
                                if (password.isNotBlank()) Text("注册密码: $password")
                            }
                            items.forEach { (k, v) ->
                                Text("$k: $v")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailDialog = null }) {
                    Text("关闭")
                }
            },
            dismissButton = null
        )
    }

    // 编辑对话框
    if (editingEntry != null) {
        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { /* 无标题 */ },
            text = {
                Column {
                    OutlinedTextField(
                        value = mainRemark,
                        onValueChange = { mainRemark = it },
                        label = { Text("客户名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editingEntry!!.address,
                        onValueChange = { newAddr ->
                            editingEntry = editingEntry!!.copy(address = newAddr)
                        },
                        label = { Text("IP 地址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customerAddress,
                        onValueChange = { customerAddress = it },
                        label = { Text("客户地址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (editingEntry!!.category == "IMS") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = imsPort,
                            onValueChange = { imsPort = it },
                            label = { Text("端口号") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = imsNumber,
                            onValueChange = { imsNumber = it },
                            label = { Text("号码") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = imsPassword,
                            onValueChange = { imsPassword = it },
                            label = { Text("注册密码") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("额外备注", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        itemsIndexed(remarkItems) { index, (key, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = key,
                                        onValueChange = { newKey ->
                                            remarkItems = remarkItems.toMutableList().apply {
                                                set(index, newKey to value)
                                            }
                                        },
                                        label = { Text("备注名称") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = value,
                                        onValueChange = { newValue ->
                                            remarkItems = remarkItems.toMutableList().apply {
                                                set(index, key to newValue)
                                            }
                                        },
                                        label = { Text("内容") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                IconButton(onClick = {
                                    remarkItems = remarkItems.toMutableList().apply { removeAt(index) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            remarkItems = remarkItems + ("" to "")
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新增备注")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingEntry?.let { entry ->
                        val json = JSONObject()
                        try {
                            val oldJson = JSONObject(entry.extraRemarks)
                            oldJson.keys().forEach { key ->
                                if (key.matches(Regex("IP\\d+"))) {
                                    json.put(key, oldJson.get(key))
                                }
                            }
                        } catch (_: Exception) {}
                        if (customerAddress.isNotBlank()) {
                            json.put("地址", customerAddress)
                        }
                        if (entry.category == "IMS") {
                            if (imsPort.isNotBlank()) json.put("ims_port", imsPort)
                            if (imsNumber.isNotBlank()) json.put("ims_number", imsNumber)
                            if (imsPassword.isNotBlank()) json.put("ims_password", imsPassword)
                        }
                        remarkItems.forEach { (k, v) ->
                            if (k.isNotBlank()) {
                                json.put(k, v)
                            }
                        }
                        val updatedEntry = entry.copy(
                            name = mainRemark,
                            extraRemarks = json.toString()
                        )
                        viewModel.updateEntry(updatedEntry)
                    }
                    editingEntry = null
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) {
                    Text("取消")
                }
            }
        )
    }
}
