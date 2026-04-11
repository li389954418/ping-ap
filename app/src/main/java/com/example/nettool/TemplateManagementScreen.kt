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
fun TemplateManagementScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val templates by viewModel.templates.collectAsState(initial = emptyList())

    var editingTemplate by remember { mutableStateOf<TemplateEntry?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("识别模板管理", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("新增模板")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(templates) { template ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(template.name, style = MaterialTheme.typography.titleMedium)
                            Text("模式: ${template.pattern}", style = MaterialTheme.typography.bodySmall)
                            Text("目标字段: ${template.targetField}", style = MaterialTheme.typography.bodySmall)
                            Text("状态: ${if (template.enabled) "启用" else "禁用"}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            IconButton(onClick = { editingTemplate = template }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                            }
                            IconButton(onClick = { viewModel.deleteTemplate(template) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
                        }
                    }
                }
            }
        }
    }

    // 新增/编辑对话框
    if (showAddDialog || editingTemplate != null) {
        var name by remember { mutableStateOf(editingTemplate?.name ?: "") }
        var pattern by remember { mutableStateOf(editingTemplate?.pattern ?: "") }
        var targetField by remember { mutableStateOf(editingTemplate?.targetField ?: "") }
        var enabled by remember { mutableStateOf(editingTemplate?.enabled ?: true) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingTemplate = null
            },
            title = { Text(if (editingTemplate == null) "新增模板" else "编辑模板") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("模板名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text("匹配模式（正则表达式）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetField,
                        onValueChange = { targetField = it },
                        label = { Text("目标字段（address/name/remark_xxx）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("启用状态")
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank() && pattern.isNotBlank() && targetField.isNotBlank()) {
                        val template = TemplateEntry(
                            id = editingTemplate?.id ?: 0,
                            name = name,
                            pattern = pattern,
                            targetField = targetField,
                            enabled = enabled
                        )
                        if (editingTemplate == null) {
                            viewModel.addTemplate(template)
                        } else {
                            viewModel.updateTemplate(template)
                        }
                        showAddDialog = false
                        editingTemplate = null
                    }
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    editingTemplate = null
                }) {
                    Text("取消")
                }
            }
        )
    }
}