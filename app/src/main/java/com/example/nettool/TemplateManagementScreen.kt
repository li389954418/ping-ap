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

    // 新增/编辑模板对话框（两阶段）
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
    var step by remember { mutableStateOf(1) } // 1: 输入文档并分词, 2: 配置规则
    var templateName by remember { mutableStateOf(initialTemplate?.name ?: "") }
    var documentText by remember { mutableStateOf("") }

    // 分词结果：词汇 -> 是否选中
    var words by remember { mutableStateOf(listOf<String>()) }
    var selectedWords by remember { mutableStateOf(setOf<String>()) }

    // 规则列表：每个选中词汇对应一个目标字段
    var rules by remember { mutableStateOf(listOf<RuleItem>()) }

    data class RuleItem(
        val keyword: String,
        var targetField: String,
        var extractUntil: String = "line" // 简化，固定提取到行尾
    )

    // 初始化：如果是编辑模式，从 rulesJson 恢复状态
    LaunchedEffect(initialTemplate) {
        initialTemplate?.let { template ->
            templateName = template.name
            val rulesList = mutableListOf<RuleItem>()
            val wordsSet = mutableSetOf<String>()
            try {
                val jsonArray = JSONArray(template.rulesJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val keyword = obj.getString("keyword")
                    val targetField = obj.getString("targetField")
                    rulesList.add(RuleItem(keyword, targetField))
                    wordsSet.add(keyword)
                }
            } catch (e: Exception) { }
            rules = rulesList
            selectedWords = wordsSet
            // 注意：编辑时 documentText 无原始文档，无法重新分词，直接进入步骤2
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
                        onClick = {
                            // 分词：按空格、换行、标点分割
                            val delimiters = Regex("[\\s,，。.、;；:：!！?？()（）\\[\\]【】\"'‘’“”\\n\\r]+")
                            words = documentText.split(delimiters)
                                .filter { it.length >= 2 } // 至少两个字符
                                .distinct()
                            step = 2
                        },
                        enabled = templateName.isNotBlank() && documentText.isNotBlank()
                    ) {
                        Text("下一步：选择关键词")
                    }
                } else {
                    // 步骤2：展示分词结果，多选，并为选中项配置字段
                    Text("选择关键词并指定字段", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    // 如果是新增且 words 为空（从步骤1过来），则显示分词结果
                    if (words.isNotEmpty() && initialTemplate == null) {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(words) { word ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedWords.contains(word),
                                        onCheckedChange = { checked ->
                                            selectedWords = if (checked) {
                                                selectedWords + word
                                            } else {
                                                selectedWords - word
                                            }
                                            // 同步更新 rules
                                            if (checked && rules.none { it.keyword == word }) {
                                                rules = rules + RuleItem(word, "")
                                            } else if (!checked) {
                                                rules = rules.filter { it.keyword != word }
                                            }
                                        }
                                    )
                                    Text(word, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        // 编辑模式或无分词结果时，直接展示已配置的规则
                        Text("已配置的关键词规则：")
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(rules) { rule ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(rule.keyword, modifier = Modifier.weight(1f))
                                    OutlinedTextField(
                                        value = rule.targetField,
                                        onValueChange = { newField ->
                                            val index = rules.indexOf(rule)
                                            if (index >= 0) {
                                                rules = rules.toMutableList().apply {
                                                    set(index, rule.copy(targetField = newField))
                                                }
                                            }
                                        },
                                        label = { Text("目标字段") },
                                        modifier = Modifier.weight(2f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // 如果是从步骤1过来的且已选中词汇，需要为它们配置字段
                    if (initialTemplate == null && selectedWords.isNotEmpty()) {
                        Text("为选中的关键词指定目标字段：")
                        rules.forEachIndexed { index, rule ->
                            if (rule.targetField.isBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(rule.keyword, modifier = Modifier.weight(1f))
                                    OutlinedTextField(
                                        value = rule.targetField,
                                        onValueChange = { newField ->
                                            rules = rules.toMutableList().apply {
                                                set(index, rule.copy(targetField = newField))
                                            }
                                        },
                                        label = { Text("字段 (如 remark_设备)") },
                                        modifier = Modifier.weight(2f),
                                        singleLine = true
                                    )
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
                    if (step == 1) {
                        // 不会执行到这里，因为步骤1的按钮已处理
                    } else {
                        // 保存：将 rules 转为 JSON
                        val jsonArray = JSONArray()
                        rules.forEach { rule ->
                            if (rule.targetField.isNotBlank()) {
                                val obj = JSONObject()
                                obj.put("keyword", rule.keyword)
                                obj.put("targetField", rule.targetField)
                                obj.put("extractUntil", "line") // 固定提取到行尾
                                jsonArray.put(obj)
                            }
                        }
                        onSave(templateName, jsonArray.toString())
                    }
                },
                enabled = when {
                    step == 1 -> false
                    else -> templateName.isNotBlank() && rules.isNotEmpty() && rules.all { it.targetField.isNotBlank() }
                }
            ) {
                Text(if (step == 1) "下一步" else "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}