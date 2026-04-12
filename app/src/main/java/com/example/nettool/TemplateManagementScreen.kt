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
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateManagementScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val templates by viewModel.templates.collectAsState(initial = emptyList())

    var showImportDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<TemplateEntry?>(null) }

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
            onClick = { showImportDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("导入模板")
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
                            val ruleCount = try {
                                JSONArray(template.rulesJson).length()
                            } catch (e: Exception) {
                                0
                            }
                            Text("包含 $ruleCount 个关键词规则", style = MaterialTheme.typography.bodySmall)
                            Text("状态: ${if (template.enabled) "启用" else "禁用"}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            IconButton(onClick = {
                                // 切换启用状态
                                viewModel.updateTemplate(template.copy(enabled = !template.enabled))
                            }) {
                                Icon(
                                    if (template.enabled) Icons.Default.CheckCircle else Icons.Default.Block,
                                    contentDescription = null
                                )
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

    // 导入模板对话框
    if (showImportDialog) {
        ImportTemplateDialog(
            onDismiss = { showImportDialog = false },
            onImport = { template ->
                viewModel.addTemplate(template)
                showImportDialog = false
            }
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
                    value = jsonContent,
                    onValueChange = { jsonContent = it },
                    label = { Text("模板 JSON") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("模板格式说明：", style = MaterialTheme.typography.titleSmall)
                Text("""{
  "name": "模板名称",
  "rules": [
    {"keyword": "关键词1", "targetField": "address"},
    {"keyword": "关键词2", "targetField": "remark_设备名"}
  ]
}""", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        val root = JSONObject(jsonContent)
                        val name = root.getString("name")
                        val rulesArray = root.getJSONArray("rules")
                        val rules = JSONArray()
                        for (i in 0 until rulesArray.length()) {
                            val rule = rulesArray.getJSONObject(i)
                            val keyword = rule.getString("keyword")
                            val targetField = rule.getString("targetField")
                            val obj = JSONObject()
                            obj.put("keyword", keyword)
                            obj.put("targetField", targetField)
                            obj.put("extractUntil", "line")
                            rules.put(obj)
                        }
                        val template = TemplateEntry(
                            name = name,
                            rulesJson = rules.toString(),
                            enabled = true
                        )
                        onImport(template)
                    } catch (e: Exception) {
                        errorMessage = "JSON 格式错误：${e.message}"
                    }
                }
            ) {
                Text("导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}