package com.example.nettool

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
fun CategoryManagementScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val protectedCategories = listOf("全部", "互联网", "供水", "医保", "IMS", "数据专线")

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("分页管理", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("新增分页")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(categories.filter { it != "全部" }) { category ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category, style = MaterialTheme.typography.titleMedium)
                        Row {
                            IconButton(onClick = { editingCategory = category }) { Icon(Icons.Default.Edit, contentDescription = "重命名") }
                            if (!protectedCategories.contains(category)) {
                                IconButton(onClick = {
                                    viewModel.deleteCategory(category)
                                }) { Icon(Icons.Default.Delete, contentDescription = "删除") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("新增分页") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("分页名称") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) viewModel.addCategory(newName)
                    showAddDialog = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }

    if (editingCategory != null) {
        var newName by remember { mutableStateOf(editingCategory!!) }
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("重命名分页") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("分页名称") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) viewModel.renameCategory(editingCategory!!, newName)
                    editingCategory = null
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { editingCategory = null }) { Text("取消") } }
        )
    }
}
