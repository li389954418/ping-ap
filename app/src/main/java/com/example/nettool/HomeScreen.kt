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
    var pingAddress by remember { mutableStateOf("") }
    var pingCount by remember { mutableStateOf("4") }
    var pingSize by remember { mutableStateOf("56") }
    var pingOutput by remember { mutableStateOf(listOf<String>()) }
    var isPinging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var traceAddress by remember { mutableStateOf("") }
    var traceOutput by remember { mutableStateOf(listOf<String>()) }
    var isTracing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Ping 区域
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("⚡ 自定义 Ping", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pingAddress,
                    onValueChange = { pingAddress = it },
                    label = { Text("目标 IP/域名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
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
                        label = { Text("包大小") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            pingOutput = emptyList()
                            isPinging = true
                            scope.launch {
                                PingUtil.pingWithFlow(
                                    address = pingAddress,
                                    params = PingUtil.PingParams(
                                        count = pingCount.toIntOrNull() ?: 4,
                                        packetSize = pingSize.toIntOrNull() ?: 56
                                    )
                                ).collect { line ->
                                    pingOutput = pingOutput + line
                                }
                                isPinging = false
                            }
                        },
                        enabled = pingAddress.isNotBlank() && !isPinging
                    ) {
                        Text(if (isPinging) "Pinging..." else "开始 Ping")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { pingOutput = emptyList() },
                        enabled = pingOutput.isNotEmpty()
                    ) {
                        Text("清空")
                    }
                }
                if (pingOutput.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            items(pingOutput) { line ->
                                Text(line, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Traceroute 区域
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🌐 路由追踪", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = traceAddress,
                    onValueChange = { traceAddress = it },
                    label = { Text("目标 IP/域名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            traceOutput = emptyList()
                            isTracing = true
                            scope.launch {
                                TracerouteUtil.traceroute(traceAddress).collect { line ->
                                    traceOutput = traceOutput + line
                                }
                                isTracing = false
                            }
                        },
                        enabled = traceAddress.isNotBlank() && !isTracing
                    ) {
                        Text(if (isTracing) "追踪中..." else "开始追踪")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { traceOutput = emptyList() },
                        enabled = traceOutput.isNotEmpty()
                    ) {
                        Text("清空")
                    }
                }
                if (traceOutput.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            items(traceOutput) { line ->
                                Text(line, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
