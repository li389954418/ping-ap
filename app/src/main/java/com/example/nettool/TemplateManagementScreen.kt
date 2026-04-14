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
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateManagementScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val templates by viewModel.templates.collectAsState(initial = emptyList())

    var showImportDialog by remember { mutableStateOf(false) }
    var editingTemplateName by remember { mutableStateOf<String?>(null) }
    var editingTemplateId by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("自动化参数模板", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showImportDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("导入模板")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(templates) { template ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(template.name, style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = {
                                    editingTemplateId = template.id
                                    editingTemplateName = template.name
                                }) { Icon(Icons.Default.Edit, contentDescription = "编辑名称") }
                            }
                            val ruleCount = try { JSONArray(template.rulesJson).length() } catch (e: Exception) { 0 }
                            Text("包含 $ruleCount 个关键词规则", style = MaterialTheme.typography.bodySmall)
                            Text("状态: ${if (template.enabled) "启用" else "禁用"}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            IconButton(onClick = { viewModel.updateTemplate(template.copy(enabled = !template.enabled)) }) {
                                Icon(if (template.enabled) Icons.Default.CheckCircle else Icons.Default.Block, contentDescription = null)
                            }
                            IconButton(onClick = { viewModel.deleteTemplate(template) }) { Icon(Icons.Default.Delete, contentDescription = "删除") }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        ImportTemplateDialog(
            onDismiss = { showImportDialog = false },
            onImport = { template ->
                viewModel.addTemplate(template)
                showImportDialog = false
            }
        )
    }

    if (editingTemplateName != null && editingTemplateId != null) {
        var newName by remember { mutableStateOf(editingTemplateName!!) }
        AlertDialog(
            onDismissRequest = { editingTemplateName = null },
            title = { Text("修改模板名称") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("模板名称") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val template = templates.find { it.id == editingTemplateId }
                    if (template != null && newName.isNotBlank()) {
                        viewModel.updateTemplate(template.copy(name = newName))
                    }
                    editingTemplateName = null
                    editingTemplateId = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editingTemplateName = null; editingTemplateId = null }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportTemplateDialog(
    onDismiss: () -> Unit,
    onImport: (TemplateEntry) -> Unit
) {
    var jsonContent by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入模板") },
        text = {
            Column {
                Text("请粘贴模板 JSON 内容：", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = jsonContent, onValueChange = { jsonContent = it },
                    label = { Text("模板 JSON") },
                    modifier = Modifier.fillMaxWidth().height(200.dp), maxLines = 10
                )
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    val template = TemplateEntry.fromJson(jsonContent)
                    onImport(template)
                } catch (e: Exception) {
                    errorMessage = "JSON 格式错误：${e.message}"
                }
            }) { Text("导入") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
