package com.example.nettool

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val versionName = remember { getVersionName(context) }

    var showLogDialog by remember { mutableStateOf(false) }
    var selectedLogContent by remember { mutableStateOf("") }
    val crashLogs = remember { CrashHandler.getCrashLogs(context) }

    var themeMode by remember { mutableStateOf("auto") }
    LaunchedEffect(Unit) {
        ThemeManager.getThemeFlow(context).collect { mode ->
            themeMode = mode
        }
    }

    var userName by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        ThemeManager.getUserNameFlow(context).collect { name ->
            userName = name
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("⚙️ 设置", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // 使用人
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👤 使用人")
                var editing by remember { mutableStateOf(false) }
                if (editing) {
                    var tempName by remember { mutableStateOf(userName) }
                    AlertDialog(
                        onDismissRequest = { editing = false },
                        title = { Text("设置使用人") },
                        text = {
                            OutlinedTextField(
                                value = tempName,
                                onValueChange = { tempName = it },
                                label = { Text("姓名或工号") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                scope.launch {
                                    ThemeManager.setUserName(context, tempName)
                                    userName = tempName
                                }
                                editing = false
                            }) { Text("保存") }
                        },
                        dismissButton = {
                            TextButton(onClick = { editing = false }) { Text("取消") }
                        }
                    )
                }
                TextButton(onClick = { editing = true }) {
                    Text(userName.ifBlank { "未设置" })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 主题配色
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🎨 配色", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("auto" to "自动", "light" to "白天", "dark" to "黑夜").forEach { (value, label) ->
                        FilterChip(
                            selected = themeMode == value,
                            onClick = {
                                scope.launch { ThemeManager.setTheme(context, value) }
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 分页管理入口
        Card(
            modifier = Modifier.fillMaxWidth().clickable { navController?.navigate(Screen.CategoryManagement.route) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📂 分页管理")
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 自动化参数模板入口
        Card(
            modifier = Modifier.fillMaxWidth().clickable { navController?.navigate(Screen.TemplateManagement.route) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📝 自动化参数模板")
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 回收站入口
        Card(
            modifier = Modifier.fillMaxWidth().clickable { navController?.navigate(Screen.RecycleBin.route) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🗑️ 回收站")
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 数据备份入口
        Card(
            modifier = Modifier.fillMaxWidth().clickable { navController?.navigate(Screen.Backup.route) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💾 数据备份")
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 关于卡片
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("关于", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("版本号：$versionName")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 崩溃日志卡片
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📄 崩溃日志", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (crashLogs.isEmpty()) {
                    Text("暂无崩溃日志", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("发现 ${crashLogs.size} 个日志文件，点击查看", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(crashLogs) { file ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedLogContent = file.readText()
                                    showLogDialog = true
                                }.padding(vertical = 4.dp)
                            ) {
                                Text(file.name, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = { Text("崩溃详情") },
            text = {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(text = selectedLogContent, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { showLogDialog = false }) { Text("关闭") } }
        )
    }
}

fun getVersionName(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "未知"
    } catch (e: Exception) {
        "未知"
    }
}

}
