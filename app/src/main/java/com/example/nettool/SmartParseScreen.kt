package com.example.nettool

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartParseScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    var previewEntries by remember { mutableStateOf<List<IpEntry>>(emptyList()) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    var editingEntry by remember { mutableStateOf<IpEntry?>(null) }
    var mainRemark by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var remarkItems by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var selectedCategory by remember { mutableStateOf("默认") }

    val categories by viewModel.categories.collectAsState(initial = emptyList())
    var categoryExpanded by remember { mutableStateOf(false) }

    fun startEditing(entry: IpEntry) {
        editingEntry = entry
        mainRemark = entry.name
        val json = try {
            JSONObject(entry.extraRemarks)
        } catch (e: Exception) {
            JSONObject()
        }
        customerAddress = json.optString("地址", "").ifBlank { json.optString("address", "") }
        val items = mutableListOf<Pair<String, String>>()
        json.keys().forEach { key ->
            if (key != "地址" && key != "address" && !key.matches(Regex("IP\\d+"))) {
                items.add(key to json.optString(key, ""))
            }
        }
        remarkItems = items
        selectedCategory = entry.category
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("智能解析", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("粘贴文档内容") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            maxLines = 10
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("选择分类") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            selectedCategory = category.name
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isProcessing = true
                previewEntries = viewModel.autoParseAndPreview(inputText, selectedCategory)
                isProcessing = false
                if (previewEntries.isNotEmpty()) {
                    startEditing(previewEntries.first())
                    showConfirmDialog = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = inputText.isNotBlank() && !isProcessing
        ) {
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("开始解析")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (previewEntries.isEmpty() && inputText.isNotBlank() && !isProcessing) {
            Text("未匹配到任何 IP 地址，无法保存", color = MaterialTheme.colorScheme.error)
        }
    }

    if (showConfirmDialog && editingEntry != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("确认并编辑") },
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
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        label = { Text("分类") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        remarkItems.forEach { (k, v) ->
                            if (k.isNotBlank()) {
                                json.put(k, v)
                            }
                        }
                        val updatedEntry = entry.copy(
                            name = mainRemark,
                            extraRemarks = json.toString(),
                            category = selectedCategory
                        )
                        viewModel.batchSaveEntries(listOf(updatedEntry))
                    }
                    showConfirmDialog = false
                    onBack()
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}