package com.example.nettool

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedListScreen(
    viewModel: MainViewModel = viewModel(),
    onItemClick: (String) -> Unit = {}
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")

    var editingEntry by remember { mutableStateOf<IpEntry?>(null) }
    var newName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("🔍 搜索备注或IP") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 列表
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(entries) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onItemClick(entry.address) },
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
                        }
                        Row {
                            IconButton(onClick = {
                                editingEntry = entry
                                newName = entry.name
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                            }
                            IconButton(onClick = { viewModel.deleteEntry(entry) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
                        }
                    }
                }
            }
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
}
