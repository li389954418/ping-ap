package com.example.nettool

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SavedListScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateToHome: () -> Unit = {} // 用于切换到底部导航的首页
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")

    var editingEntry by remember { mutableStateOf<IpEntry?>(null) }
    var newName by remember { mutableStateOf("") }
    var showDetailDialog by remember { mutableStateOf<IpEntry?>(null) }

    // 滑动删除状态
    val dismissStateMap = remember { mutableStateMapOf<Int, DismissState>() }

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
                val dismissState = dismissStateMap.getOrPut(entry.id) {
                    DismissState(DismissValue.Default)
                }

                SwipeToDismiss(
                    state = dismissState,
                    directions = setOf(DismissDirection.EndToStart),
                    dismissThresholds = { FractionalThreshold(0.5f) },
                    background = {
                        val color = when (dismissState.dismissDirection) {
                            DismissDirection.EndToStart -> Color.Red
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (dismissState.dismissDirection == DismissDirection.EndToStart) {
                                Row(horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = {
                                        editingEntry = entry
                                        newName = entry.name
                                        dismissState.reset()
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.White)
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteEntry(entry)
                                        dismissStateMap.remove(entry.id)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.White)
                                    }
                                }
                            }
                        }
                    },
                    dismissContent = {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    // 点击卡片：自动跳转首页并开始 Ping
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
                                    // 预留名称和地址字段（暂为空）
                                    Text("名称: —", style = MaterialTheme.typography.bodySmall)
                                    Text("地址: —", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { showDetailDialog = entry }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "详情")
                                }
                            }
                        }
                    }
                )

                // 重置滑动状态当条目改变时
                LaunchedEffect(entry) {
                    dismissState.reset()
                }
            }
        }
    }

    // 详情弹窗
    showDetailDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDetailDialog = null },
            title = { Text("地址详情") },
            text = {
                Column {
                    Text("备注: ${entry.name}")
                    Text("IP/域名: ${entry.address}")
                    Text("名称: —")
                    Text("地址: —")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingEntry = entry
                    newName = entry.name
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

    // 编辑备注对话框
    if (editingEntry != null) {
        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text("修改备注") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("备注名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editingEntry?.let { entry ->
                        viewModel.addEntry(newName, entry.address)
                        viewModel.deleteEntry(entry)
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
