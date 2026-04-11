package com.example.nettool

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val versionName = remember { getVersionName(context) }

    var showLogDialog by remember { mutableStateOf(false) }
    var selectedLogContent by remember { mutableStateOf("") }
    val crashLogs = remember { CrashHandler.getCrashLogs(context) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("⚙️ 设置", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // 关于卡片
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("关于", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("版本号：$versionName")
                Text("作者：你的名字")
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLogContent = file.readText()
                                        showLogDialog = true
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(file.name, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    // 日志内容弹窗
    if (showLogDialog) {
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = { Text("崩溃详情") },
            text = {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        text = selectedLogContent,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogDialog = false }) {
                    Text("关闭")
                }
            }
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
