package com.example.nettool

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showAutoBackupConfig by remember { mutableStateOf(false) }

    // 导出选项
    var exportEntries by remember { mutableStateOf(true) }
    var exportTemplates by remember { mutableStateOf(true) }

    // 导入数据预览
    var importPreview by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var importJson by remember { mutableStateOf("") }

    // 自动备份配置
    var autoBackupEnabled by remember { mutableStateOf(BackupManager.isAutoBackupEnabled(context)) }
    var autoBackupTrigger by remember { mutableStateOf(BackupManager.getAutoBackupTrigger(context)) }
    var autoBackupTarget by remember { mutableStateOf(BackupManager.getAutoBackupTarget(context)) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("💾 数据备份", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 导出卡片
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📤 导出数据", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showExportDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("导出到本地")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 导入卡片
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📥 导入数据", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showImportDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("从本地导入")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 自动备份卡片
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔄 自动备份", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = autoBackupEnabled, onCheckedChange = {
                        autoBackupEnabled = it
                        BackupManager.setAutoBackupEnabled(context, it)
                    })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showAutoBackupConfig = true }, modifier = Modifier.fillMaxWidth(), enabled = autoBackupEnabled) {
                    Text("配置自动备份")
                }
            }
        }
    }

    // 导出对话框
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("导出数据") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = exportEntries, onCheckedChange = { exportEntries = it })
                        Text("存储条目")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = exportTemplates, onCheckedChange = { exportTemplates = it })
                        Text("模板")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val data = viewModel.exportAllData()
                        val success = BackupManager.saveToFile(context, data, "NetTool_Backup_${System.currentTimeMillis()}.json")
                        Toast.makeText(context, if (success) "导出成功" else "导出失败", Toast.LENGTH_SHORT).show()
                    }
                    showExportDialog = false
                }) { Text("导出") }
            },
            dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text("取消") } }
        )
    }

    // 导入对话框
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入数据") },
            text = {
                Column {
                    OutlinedTextField(
                        value = importJson, onValueChange = { importJson = it },
                        label = { Text("粘贴备份 JSON") },
                        modifier = Modifier.fillMaxWidth().height(150.dp), maxLines = 8
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        try {
                            val root = JSONObject(importJson)
                            val entriesCount = root.optJSONArray("entries")?.length() ?: 0
                            val templatesCount = root.optJSONArray("templates")?.length() ?: 0
                            importPreview = entriesCount to templatesCount
                        } catch (e: Exception) {
                            Toast.makeText(context, "无效的 JSON 格式", Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("预览数据")
                    }
                    if (importPreview != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("将导入 ${importPreview!!.first} 个条目，${importPreview!!.second} 个模板")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (importJson.isNotBlank()) {
                        scope.launch {
                            viewModel.importData(importJson, importEntries = true, importTemplates = true)
                            Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showImportDialog = false
                    importJson = ""
                    importPreview = null
                }) { Text("导入") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    importJson = ""
                    importPreview = null
                }) { Text("取消") }
            }
        )
    }

    // 自动备份配置对话框
    if (showAutoBackupConfig) {
        AlertDialog(
            onDismissRequest = { showAutoBackupConfig = false },
            title = { Text("自动备份配置") },
            text = {
                Column {
                    Text("触发方式", style = MaterialTheme.typography.titleSmall)
                    listOf("data_change" to "数据变动时", "app_start" to "打开软件时", "manual" to "手动").forEach { (value, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = autoBackupTrigger == value, onClick = {
                                autoBackupTrigger = value
                                BackupManager.setAutoBackupTrigger(context, value)
                            })
                            Text(label)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("备份目标", style = MaterialTheme.typography.titleSmall)
                    listOf("local" to "本地", "webdav" to "坚果云 WebDAV").forEach { (value, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = autoBackupTarget == value, onClick = {
                                autoBackupTarget = value
                                BackupManager.setAutoBackupTarget(context, value)
                            })
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAutoBackupConfig = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showAutoBackupConfig = false }) { Text("取消") } }
        )
    }
}
