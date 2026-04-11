package com.example.nettool

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    // Ping 参数
    var targetAddress by remember { mutableStateOf("") }
    var pingCount by remember { mutableStateOf("0") }
    var pingSize by remember { mutableStateOf("56") }

    // 输出结果
    var outputLines by remember { mutableStateOf(listOf<String>()) }
    var isRunning by remember { mutableStateOf(false) }
    var pingJob by remember { mutableStateOf<Job?>(null) }

    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 目标地址输入
        OutlinedTextField(
            value = targetAddress,
            onValueChange = { targetAddress = it },
            label = { Text("目标 IP 或域名") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Ping 参数：次数 + 包大小
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = pingCount,
                onValueChange = { pingCount = it },
                label = { Text("次数 (0=长Ping)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = pingSize,
                onValueChange = { pingSize = it },
                label = { Text("包大小 (字节)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 操作按钮：开始/停止 + 清空
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 开始 / 停止按钮
            Button(
                onClick = {
                    if (isRunning) {
                        // 停止：取消协程
                        pingJob?.cancel()
                        isRunning = false
                        outputLines = outputLines + "\n--- 已停止 ---"
                    } else {
                        // 开始
                        outputLines = emptyList()
                        isRunning = true
                        pingJob = scope.launch {
                            try {
                                PingNative.ping(
                                    host = targetAddress,
                                    count = pingCount.toIntOrNull() ?: 4,
                                    packetSize = pingSize.toIntOrNull() ?: 56
                                ).collect { line ->
                                    outputLines = outputLines + line
                                }
                            } catch (e: CancellationException) {
                                outputLines = outputLines + "\n--- 已取消 ---"
                            } catch (e: Exception) {
                                outputLines = outputLines + "\n发生错误: ${e.message}"
                            } finally {
                                isRunning = false
                                pingJob = null
                            }
                        }
                    }
                },
                enabled = targetAddress.isNotBlank(),
                modifier = Modifier.width(120.dp)
            ) {
                Text(if (isRunning) "停止" else "开始")
            }

            // 清空按钮
            Button(
                onClick = { outputLines = emptyList() },
                enabled = outputLines.isNotEmpty() && !isRunning,
                modifier = Modifier.width(120.dp)
            ) {
                Text("清空")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 结果显示区域
        Text(
            text = "📡 Ping 结果",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            SelectionContainer {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    reverseLayout = true
                ) {
                    items(outputLines) { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
