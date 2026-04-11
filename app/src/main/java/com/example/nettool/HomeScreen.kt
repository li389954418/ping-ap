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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel = viewModel()) {
    var useICMP by remember { mutableStateOf(true) }
    var targetAddress by remember { mutableStateOf("") }
    var pingCount by remember { mutableStateOf("0") }
    var pingSize by remember { mutableStateOf("56") }
    var pingPort by remember { mutableStateOf("80") }

    var outputLines by remember { mutableStateOf(listOf<String>()) }
    var isRunning by remember { mutableStateOf(false) }
    var pingJob by remember { mutableStateOf<Job?>(null) }

    val scope = rememberCoroutineScope()

    // 监听存储页选择的地址（仅填充输入框，不自动开始）
    val selectedAddress by viewModel.selectedAddress.collectAsState()
    LaunchedEffect(selectedAddress) {
        if (selectedAddress.isNotBlank()) {
            targetAddress = selectedAddress
            viewModel.setSelectedAddress("")
        }
    }

    // 监听自动 Ping 请求（填充并自动开始）
    val autoPingAddress by viewModel.autoPingAddress.collectAsState()
    LaunchedEffect(autoPingAddress) {
        if (autoPingAddress.isNotBlank()) {
            targetAddress = autoPingAddress
            // 自动开始 Ping（模拟点击开始按钮）
            if (!isRunning) {
                startPing(
                    address = autoPingAddress,
                    useICMP = useICMP,
                    pingCount = pingCount,
                    pingSize = pingSize,
                    pingPort = pingPort,
                    scope = scope,
                    onStart = { job -> pingJob = job; isRunning = true },
                    onLine = { line -> outputLines = outputLines + line },
                    onFinish = { isRunning = false; pingJob = null },
                    onCancel = { isRunning = false; pingJob = null }
                )
            }
            viewModel.clearAutoPing()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 目标地址输入 + 保存按钮
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = targetAddress,
                onValueChange = { targetAddress = it },
                label = { Text("目标 IP 或域名") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            TextButton(
                onClick = {
                    if (targetAddress.isNotBlank()) {
                        viewModel.addEntry(targetAddress, targetAddress)
                    }
                }
            ) {
                Text("保存")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 协议切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = useICMP,
                onClick = { useICMP = true },
                label = { Text("ICMP (系统)") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = !useICMP,
                onClick = { useICMP = false },
                label = { Text("TCP (端口)") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 参数区域
        if (useICMP) {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = pingCount,
                    onValueChange = { pingCount = it },
                    label = { Text("次数 (0=长)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = pingSize,
                    onValueChange = { pingSize = it },
                    label = { Text("包大小") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = pingCount,
                    onValueChange = { pingCount = it },
                    label = { Text("次数 (0=长)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = pingPort,
                    onValueChange = { pingPort = it },
                    label = { Text("端口") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    if (isRunning) {
                        pingJob?.cancel()
                    } else {
                        startPing(
                            address = targetAddress,
                            useICMP = useICMP,
                            pingCount = pingCount,
                            pingSize = pingSize,
                            pingPort = pingPort,
                            scope = scope,
                            onStart = { job -> pingJob = job; isRunning = true; outputLines = emptyList() },
                            onLine = { line -> outputLines = outputLines + line },
                            onFinish = { isRunning = false; pingJob = null },
                            onCancel = { outputLines = outputLines + "\n--- 已停止 ---"; isRunning = false; pingJob = null }
                        )
                    }
                },
                enabled = targetAddress.isNotBlank(),
                modifier = Modifier.width(120.dp)
            ) {
                Text(if (isRunning) "停止" else "开始")
            }

            Button(
                onClick = { outputLines = emptyList() },
                enabled = outputLines.isNotEmpty() && !isRunning,
                modifier = Modifier.width(120.dp)
            ) {
                Text("清空")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (useICMP) "📡 ICMP Ping 结果" else "🔌 TCP Ping 结果 (端口 ${pingPort.ifBlank { "80" }})",
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

// 抽取启动 Ping 的逻辑，便于复用
private fun startPing(
    address: String,
    useICMP: Boolean,
    pingCount: String,
    pingSize: String,
    pingPort: String,
    scope: CoroutineScope,
    onStart: (Job) -> Unit,
    onLine: (String) -> Unit,
    onFinish: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    onStart(scope.launch {
        try {
            val flow = if (useICMP) {
                IcmpPing.ping(
                    host = address,
                    count = pingCount.toIntOrNull() ?: 0,
                    packetSize = pingSize.toIntOrNull() ?: 56
                )
            } else {
                TcpPing.ping(
                    host = address,
                    count = pingCount.toIntOrNull() ?: 0,
                    port = pingPort.toIntOrNull() ?: 80
                )
            }
            flow.collect { line ->
                onLine(line)
            }
        } catch (e: CancellationException) {
            onCancel?.invoke()
        } catch (e: Exception) {
            onLine("\n发生错误: ${e.message}")
        } finally {
            onFinish()
        }
    })
}
