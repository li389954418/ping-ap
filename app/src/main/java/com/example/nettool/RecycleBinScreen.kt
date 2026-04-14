package com.example.nettool

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val deletedEntries by viewModel.recycleBinEntries.collectAsState(initial = emptyList())

    var showClearAllDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🗑️ 回收站", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (deletedEntries.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showClearAllDialog = true }) {
                    Text("清空回收站", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (deletedEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("回收站为空", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(deletedEntries, key = { it.id }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.name.ifBlank { "未命名" }, style = MaterialTheme.typography.bodyLarge)
                                Text(entry.address, style = MaterialTheme.typography.bodySmall)
                                Text("分类: ${entry.category}", style = MaterialTheme.typography.bodySmall)
                            }
                            Row {
                                IconButton(onClick = { viewModel.restoreEntry(entry) }) {
                                    Icon(Icons.Default.Restore, contentDescription = "恢复")
                                }
                                IconButton(onClick = { viewModel.permanentlyDeleteEntry(entry) }) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = "彻底删除")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("清空回收站") },
            text = { Text("确定要彻底删除回收站中的所有条目吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.permanentlyDeleteAllDeleted()
                    showClearAllDialog = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showClearAllDialog = false }) { Text("取消") } }
        )
    }
}
