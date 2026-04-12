package com.example.nettool

import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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

    var showAddDialog by remember { mutableStateOf(false) }
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
                            val ruleCount = try {
                                JSONArray(template.rulesJson).length()
                            } catch (e: Exception) {
                                0
                            }
                            Text("包含 $ruleCount 个关键词规则", style = MaterialTheme.typography.bodySmall)
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

    if (showAddDialog || editingTemplate != null) {
        TemplateEditDialog(
            initialTemplate = editingTemplate,
            onDismiss = {
                showAddDialog = false
                editingTemplate = null
            },
            onSave = { name, rulesJson ->
                if (editingTemplate == null) {
                    viewModel.addTemplate(TemplateEntry(name = name, rulesJson = rulesJson, enabled = true))
                } else {
                    viewModel.updateTemplate(editingTemplate!!.copy(name = name, rulesJson = rulesJson))
                }
                showAddDialog = false
                editingTemplate = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditDialog(
    initialTemplate: TemplateEntry?,
    onDismiss: () -> Unit,
    onSave: (name: String, rulesJson: String) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var templateName by remember { mutableStateOf(initialTemplate?.name ?: "") }
    var documentText by remember { mutableStateOf("") }

    // 规则项列表
    data class RuleItem(val keyword: String, var targetField: String)
    val rules = remember { mutableStateListOf<RuleItem>() }

    // 用于获取 TextView 中当前选中的文本
    var textViewRef by remember { mutableStateOf<TextView?>(null) }
    var selectedText by remember { mutableStateOf("") }

    // 初始化编辑模式
    LaunchedEffect(initialTemplate) {
        initialTemplate?.let { template ->
            templateName = template.name
            documentText = "" // 编辑时不保存原文
            val rulesList = mutableListOf<RuleItem>()
            try {
                val jsonArray = JSONArray(template.rulesJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val keyword = obj.getString("keyword")
                    val targetField = obj.getString("targetField")
                    rulesList.add(RuleItem(keyword, targetField))
                }
            } catch (_: Exception) { }
            rules.clear()
            rules.addAll(rulesList)
            step = 2
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTemplate == null) "新增模板" else "编辑模板") },
        text = {
            Column {
                if (step == 1) {
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("模板名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = documentText,
                        onValueChange = { documentText = it },
                        label = { Text("粘贴示例文档") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        maxLines = 10
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { step = 2 },
                        enabled = templateName.isNotBlank() && documentText.isNotBlank()
                    ) {
                        Text("下一步：滑动选词")
                    }
                } else {
                    Text("在原文中滑动选择关键词", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    // 使用 AndroidView 嵌入原生 TextView，支持自由文本选择
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 180.dp)
                    ) {
                        AndroidView(
                            factory = { context ->
                                TextView(context).apply {
                                    text = documentText
                                    setTextIsSelectable(true)  // 启用选择功能
                                    // 监听选择变化，实时更新 selectedText
                                    setOnClickListener { } // 确保可以获取焦点
                                    post {
                                        // 通过反射或自定义监听获取选择内容，这里使用更简单的方式：
                                        // 在外部按钮点击时通过 textViewRef 获取
                                    }
                                }
                            },
                            update = { textView ->
                                textView.text = documentText
                            },
                            modifier = Modifier.fillMaxSize()
                        ) { textView ->
                            // 保存引用供外部使用
                            LaunchedEffect(textView) {
                                textViewRef = textView
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 获取当前选中文本并添加为关键词
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("当前选中：${selectedText.ifBlank { "无" }}", style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = {
                                textViewRef?.let { tv ->
                                    val start = tv.selectionStart
                                    val end = tv.selectionEnd
                                    if (start >= 0 && end > start) {
                                        val selected = tv.text.subSequence(start, end).toString()
                                        if (selected.isNotBlank() && rules.none { it.keyword == selected }) {
                                            rules.add(RuleItem(selected, ""))
                                            selectedText = selected
                                        }
                                    }
                                }
                            },
                            enabled = textViewRef != null
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加选中")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("已添加的关键词 (${rules.size})", style = MaterialTheme.typography.titleSmall)
                    if (rules.isEmpty()) {
                        Text("暂无，请在上方选择文本后点击“添加选中”", style = MaterialTheme.typography.bodySmall)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(rules.size) { index ->
                                val rule = rules[index]
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(rule.keyword, modifier = Modifier.weight(1f))
                                    OutlinedTextField(
                                        value = rule.targetField,
                                        onValueChange = { newField ->
                                            rules[index] = rule.copy(targetField = newField)
                                        },
                                        label = { Text("目标字段") },
                                        modifier = Modifier.weight(2f),
                                        singleLine = true
                                    )
                                    IconButton(onClick = {
                                        rules.removeAt(index)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (step == 2) {
                        val jsonArray = JSONArray()
                        rules.forEach { rule ->
                            if (rule.targetField.isNotBlank()) {
                                val obj = JSONObject()
                                obj.put("keyword", rule.keyword)
                                obj.put("targetField", rule.targetField)
                                obj.put("extractUntil", "line")
                                jsonArray.put(obj)
                            }
                        }
                        onSave(templateName, jsonArray.toString())
                    }
                },
                enabled = when (step) {
                    1 -> false
                    else -> templateName.isNotBlank() && rules.isNotEmpty() && rules.all { it.targetField.isNotBlank() }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}