package com.example.nettool

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

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
    var showWebDavConfig by remember { mutableStateOf(false) }

    var exportEntries by remember { mutableStateOf(true) }
    var exportTemplates by remember { mutableStateOf(true) }

    var importPreview by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var importFile by remember { mutableStateOf<File?>(null) }

    var autoBackupEnabled by remember { mutableStateOf(BackupManager.isAutoBackupEnabled(context)) }
    var autoBackupTrigger by remember { mutableStateOf(BackupManager.getAutoBackupTrigger(context)) }
    var autoBackupTarget by remember { mutableStateOf(BackupManager.getAutoBackupTarget(context)) }

    // WebDAV 配置
    var webDavConfig by remember { mutableStateOf(BackupManager.getWebDavConfig(context)) }
    var showWebDavEditDialog by remember { mutableStateOf(false) }

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                try {
                    context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                        val content = reader.readText()
                        val root = JSONObject(content)
                        val entriesCount = root.optJSONArray("entries")?.length() ?: 0
                        val templatesCount = root.optJSONArray("templates")?.length() ?: 0
                        importPreview = entriesCount to templatesCount
                        // 保存文件引用
                        importFile = File(it.path ?: "backup.json")
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "无效的备份文件", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

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
                    Text("导出到文件")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 导入卡片
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📥 导入数据", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { filePickerLauncher.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("选择文件导入")
                }
                if (importPreview != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("将导入 ${importPreview!!.first} 个条目，${importPreview!!.second} 个模板")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    importFile?.let { file ->
                                        val content = file.readText()
                                        viewModel.importData(content, importEntries = true, importTemplates = true)
                                        Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
                                        importPreview = null
                                        importFile = null
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("确认导入") }
                        Button(
                            onClick = { importPreview = null; importFile = null },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) { Text("取消") }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // WebDAV 配置卡片
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("☁️ 坚果云 WebDAV", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showWebDavEditDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "配置")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (webDavConfig != null) {
                    Text("已配置", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("未配置", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
                        val fileName = "NetTool_Backup_${System.currentTimeMillis()}.json"
                        val success = BackupManager.saveToFile(context, data, fileName)
                        Toast.makeText(context, if (success) "导出成功: $fileName" else "导出失败", Toast.LENGTH_SHORT).show()
                    }
                    showExportDialog = false
                }) { Text("导出") }
            },
            dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text("取消") } }
        )
    }

    // WebDAV 配置编辑对话框
    if (showWebDavEditDialog) {
        var serverUrl by remember { mutableStateOf(webDavConfig?.serverUrl ?: "https://dav.jianguoyun.com/dav/") }
        var username by remember { mutableStateOf(webDavConfig?.username ?: "") }
        var password by remember { mutableStateOf(webDavConfig?.password ?: "") }

        AlertDialog(
            onDismissRequest = { showWebDavEditDialog = false },
            title = { Text("配置坚果云 WebDAV") },
            text = {
                Column {
                    OutlinedTextField(
                        value = serverUrl, onValueChange = { serverUrl = it },
                        label = { Text("服务器地址") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = username, onValueChange = { username = it },
                        label = { Text("账号（邮箱）") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("应用密码") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("应用密码需要在坚果云网页端「安全选项」中生成，不是登录密码。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val config = WebDavConfig(serverUrl, username, password)
                    BackupManager.saveWebDavConfig(context, config)
                    webDavConfig = config
                    showWebDavEditDialog = false
                    Toast.makeText(context, "配置已保存", Toast.LENGTH_SHORT).show()
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showWebDavEditDialog = false }) { Text("取消") } }
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
