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
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    // 共享的目标地址
    var targetAddress by remember { mutableStateOf("") }

    // Ping 参数
    var pingCount by remember { mutableStateOf("4") }
    var pingSize by remember { mutableStateOf("56") }

    // Traceroute 参数
    var maxHops by remember { mutableStateOf("30") }

    // 当前激活的功能：ping 或 traceroute
    var activeFunction by remember { mutableStateOf<FunctionType>(FunctionType.PING) }

    // 共享的输出结果
    var outputLines by remember { mutableStateOf(listOf<String>()) }
    var isRunning by remember { mutableStateOf(false) }

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

        // 参数区域：根据激活功能显示不同参数
        when (activeFunction) {
            FunctionType.PING -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = pingCount,
                        onValueChange = { pingCount = it },
                        label = { Text("次数") },
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
            }
            FunctionType.TRACEROUTE -> {
                OutlinedTextField(
                    value = maxHops,
                    onValueChange = { maxHops = it },
                    label = { Text("最大跳数") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 功能切换标签 + 执行按钮 + 清空按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 切换标签
            Row {
                FilterChip(
                    selected = activeFunction == FunctionType.PING,
                    onClick = { activeFunction = FunctionType.PING },
                    label = { Text("Ping") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = activeFunction == FunctionType.TRACEROUTE,
                    onClick = { activeFunction = FunctionType.TRACEROUTE },
                    label = { Text("Traceroute") }
                )
            }
            // 执行和清空按钮
            Row {
                Button(
                    onClick = {
                        outputLines = emptyList()
                        isRunning = true
                        scope.launch {
                            when (activeFunction) {
                                FunctionType.PING -> {
                                    PingUtil.pingWithFlow(
                                        address = targetAddress,
                                        params = PingUtil.PingParams(
                                            count = pingCount.toIntOrNull() ?: 4,
                                            packetSize = pingSize.toIntOrNull() ?: 56
                                        )
                                    ).collect { line ->
                                        outputLines = outputLines + line
                                    }
                                }
                                FunctionType.TRACEROUTE -> {
                                    TracerouteUtil.traceroute(
                                        host = targetAddress,
                                        maxHops = maxHops.toIntOrNull() ?: 30
                                    ).collect { line ->
                                        outputLines = outputLines + line
                                    }
                                }
                            }
                            isRunning = false
                        }
                    },
                    enabled = targetAddress.isNotBlank() && !isRunning
                ) {
                    Text(if (isRunning) "执行中..." else "开始")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { outputLines = emptyList() },
                    enabled = outputLines.isNotEmpty()
                ) {
                    Text("清空")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 共享结果显示区域（带标题）
        Text(
            text = if (activeFunction == FunctionType.PING) "📡 Ping 结果" else "🌐 Traceroute 结果",
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
                    reverseLayout = true // 最新输出在底部
                ) {
                    items(outputLines) { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

enum class FunctionType {
    PING,
    TRACEROUTE
}
