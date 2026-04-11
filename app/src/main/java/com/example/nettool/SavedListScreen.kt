package com.example.nettool

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SavedListScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateToHome: () -> Unit = {}
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")

    var editingEntry by remember { mutableStateOf<IpEntry?>(null) }
    var showDetailDialog by remember { mutableStateOf<IpEntry?>(null) }

    var remarkItems by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var mainRemark by remember { mutableStateOf("") }

    fun startEditing(entry: IpEntry) {
        editingEntry = entry
        mainRemark = entry.name
        val json = try {
            JSONObject(entry.extraRemarks)
        } catch (e: Exception) {
            JSONObject()
        }
        val items = mutableListOf<Pair<String, String>>()
        json.keys().forEach { key ->
            items.add(key to json.getString(key))
        }
        remarkItems = items
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                val dismissState = rememberDismissState(
                    confirmStateChange = { dismissValue ->
                        when (dismissValue) {
                            DismissValue.DismissedToEnd -> {
                                startEditing(entry)
                                false // 不自动移除条目
                            }
                            DismissValue.DismissedToStart -> {
                                viewModel.deleteEntry(entry)
                                true // 条目会被移除，由数据变化触发UI更新
                            }
                            else -> false
                        }
                    }
                )

                SwipeToDismiss(
                    state = dismissState,
                    directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
                    background = {
                        val direction = dismissState.dismissDirection
                        val color = when (direction) {
                            DismissDirection.StartToEnd -> Color(0xFF4CAF50)
                            DismissDirection.EndToStart -> Color(0xFFF44336)
                            else -> Color.Transparent
                        }
                        Box(modifier = Modifier.fillMaxSize().background(color)) {
                            when (direction) {
                                DismissDirection.StartToEnd -> {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "编辑",
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp)
                                    )
                                }
                                DismissDirection.EndToStart -> {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)
                                    )
                                }
                                else -> {}
                            }
                        }
                    },
                    dismissContent = {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.triggerAutoPing(entry.address)
                                    onNavigateToHome()
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
                                    Text(entry.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(entry.address, style = MaterialTheme.typography.bodySmall)
                                    val extraCount = try {
                                        JSONObject(entry.extraRemarks).length()
                                    } catch (e: Exception) {
                                        0
                                    }
                                    if (extraCount > 0) {
                                        Text("额外备注: $extraCount 项", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                IconButton(onClick = { showDetailDialog = entry }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "详情")
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    // 详情弹窗（同上）
    showDetailDialog?.let { entry ->
        val extraJson = try {
            JSONObject(entry.extraRemarks)
        } catch (e: Exception) {
            JSONObject()
        }
        val items = mutableListOf<Pair<String, String>>()
        extraJson.keys().forEach { key ->
            items.add(key to extraJson.getString(key))
        }

        AlertDialog(
            onDismissRequest = { showDetailDialog = null },
            title = { Text("地址详情") },
            text = {
                Column {
                    Text("主备注: ${entry.name}")
                    Text("IP/域名: ${entry.address}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("额外备注:", style = MaterialTheme.typography.titleSmall)
                    if (items.isEmpty()) {
                        Text("  无")
                    } else {
                        items.forEach { (k, v) ->
                            Text("  $k: $v")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    startEditing(entry)
                    showDetailDialog = null
                }) {
                    Text("编辑")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetailDialog = null }) {
                    Text("关闭")
                }
            }
        )
    }

    // 编辑对话框（多备注）
    if (editingEntry != null) {
        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text("编辑备注") },
            text = {
                Column {
                    OutlinedTextField(
                        value = mainRemark,
                        onValueChange = { mainRemark = it },
                        label = { Text("主备注名称") },
                        singleLine = true,
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
                                    Text("$key: $value")
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newKey,
                            onValueChange = { newKey = it },
                            label = { Text("键") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = newValue,
                            onValueChange = { newValue = it },
                            label = { Text("值") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            if (newKey.isNotBlank() && newValue.isNotBlank()) {
                                remarkItems = remarkItems + (newKey to newValue)
                                newKey = ""
                                newValue = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "添加")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingEntry?.let { entry ->
                        val json = JSONObject()
                        remarkItems.forEach { (k, v) ->
                            json.put(k, v)
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
