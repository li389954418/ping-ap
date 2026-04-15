package com.example.nettool

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateToSmartParse: () -> Unit = {},
    onNavigateToSavedList: () -> Unit = {}
) {
    var useICMP by remember { mutableStateOf(true) }
    var targetAddress by remember { mutableStateOf("") }
    var pingCount by remember { mutableStateOf("0") }
    var pingSize by remember { mutableStateOf("56") }
    var pingPort by remember { mutableStateOf("80") }

    var outputLines by remember { mutableStateOf(listOf<String>()) }
    var isRunning by remember { mutableStateOf(false) }
    var pingJob by remember { mutableStateOf<Job?>(null) }

    var pingTimes by remember { mutableStateOf(listOf<Double>()) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val allEntries by viewModel.entries.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember(searchQuery, allEntries) {
        if (searchQuery.isBlank()) emptyList()
        else allEntries.filter { entry ->
            entry.name.contains(searchQuery, ignoreCase = true) ||
            entry.address.contains(searchQuery, ignoreCase = true) ||
            (try {
                val json = JSONObject(entry.extraRemarks)
                json.keys().asSequence().any { key ->
                    json.optString(key).contains(searchQuery, ignoreCase = true)
                }
            } catch (e: Exception) { false })
        }.filter { viewModel.isCategoryAllowPing(it.category) }
    }
    var showDropdown by remember { mutableStateOf(false) }

    val selectedAddress by viewModel.selectedAddress.collectAsState()
    LaunchedEffect(selectedAddress) {
        if (selectedAddress.isNotBlank()) {
            targetAddress = selectedAddress
            viewModel.setSelectedAddress("")
        }
    }

    val autoPingAddress by viewModel.autoPingAddress.collectAsState()
    LaunchedEffect(autoPingAddress) {
        if (autoPingAddress.isNotBlank()) {
            targetAddress = autoPingAddress
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
                    onTime = { time -> pingTimes = (pingTimes + time).takeLast(50) },
                    onFinish = { isRunning = false; pingJob = null },
                    onCancel = { isRunning = false; pingJob = null },
                    onSummary = { summary -> outputLines = outputLines + summary }
                )
            }
            viewModel.clearAutoPing()
        }
    }

    LaunchedEffect(outputLines.size) {
        if (outputLines.isNotEmpty()) {
            listState.animateScrollToItem(outputLines.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // IP 输入框 + TCP 按钮同行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 输入框占据剩余宽度
            ExposedDropdownMenuBox(
                expanded = showDropdown && searchResults.isNotEmpty(),
                onExpandedChange = { showDropdown = it && searchQuery.isNotBlank() },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = targetAddress,
                    onValueChange = {
                        targetAddress = it
                        searchQuery = it
                        showDropdown = it.isNotBlank()
                    },
                    label = { Text("输入 IP/域名 或 搜索") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (targetAddress.isNotBlank() && !isRunning) {
                                startPing(
                                    address = targetAddress,
                                    useICMP = useICMP,
                                    pingCount = pingCount,
                                    pingSize = pingSize,
                                    pingPort = pingPort,
                                    scope = scope,
                                    onStart = { job -> pingJob = job; isRunning = true; outputLines = emptyList(); pingTimes = emptyList() },
                                    onLine = { line -> outputLines = outputLines + line },
                                    onTime = { time -> pingTimes = (pingTimes + time).takeLast(50) },
                                    onFinish = { isRunning = false; pingJob = null },
                                    onCancel = { isRunning = false; pingJob = null },
                                    onSummary = { summary -> outputLines = outputLines + summary }
                                )
                            }
                        }
                    )
                )
                ExposedDropdownMenu(
                    expanded = showDropdown && searchResults.isNotEmpty(),
                    onDismissRequest = { showDropdown = false }
                ) {
                    searchResults.forEach { entry ->
                        DropdownMenuItem(
                            text = {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.name, fontWeight = FontWeight.Medium)
                                        Text(entry.address, style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = {
                                        viewModel.requestEditEntry(entry.id)
                                        onNavigateToSavedList()
                                        showDropdown = false
                                        targetAddress = ""
                                        searchQuery = ""
                                    }) { Icon(Icons.Default.Edit, contentDescription = "编辑") }
                                }
                            },
                            onClick = {
                                targetAddress = entry.address
                                searchQuery = ""
                                showDropdown = false
                                if (!isRunning) {
                                    startPing(
                                        address = entry.address,
                                        useICMP = useICMP,
                                        pingCount = pingCount,
                                        pingSize = pingSize,
                                        pingPort = pingPort,
                                        scope = scope,
                                        onStart = { job -> pingJob = job; isRunning = true; outputLines = emptyList(); pingTimes = emptyList() },
                                        onLine = { line -> outputLines = outputLines + line },
                                        onTime = { time -> pingTimes = (pingTimes + time).takeLast(50) },
                                        onFinish = { isRunning = false; pingJob = null },
                                        onCancel = { isRunning = false; pingJob = null },
                                        onSummary = { summary -> outputLines = outputLines + summary }
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // TCP 按钮，固定宽度，不再拉伸
            Button(
                onClick = { useICMP = !useICMP },
                modifier = Modifier.width(60.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!useICMP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (!useICMP) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("TCP", maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                            onStart = { job -> pingJob = job; isRunning = true; outputLines = emptyList(); pingTimes = emptyList() },
                            onLine = { line -> outputLines = outputLines + line },
                            onTime = { time -> pingTimes = (pingTimes + time).takeLast(50) },
                            onFinish = { isRunning = false; pingJob = null },
                            onCancel = { isRunning = false; pingJob = null },
                            onSummary = { summary -> outputLines = outputLines + summary }
                        )
                    }
                },
                enabled = targetAddress.isNotBlank(),
                modifier = Modifier.width(120.dp)
            ) {
                Text(if (isRunning) "停止" else "开始")
            }

            Button(
                onClick = { outputLines = emptyList(); pingTimes = emptyList() },
                enabled = outputLines.isNotEmpty() && !isRunning,
                modifier = Modifier.width(120.dp)
            ) {
                Text("清空")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 上栏：文本输出结果
        Card(
            modifier = Modifier.fillMaxWidth().weight(2f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    reverseLayout = false
                ) {
                    items(outputLines) { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 下栏：波形图
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            PingChart(
                times = pingTimes,
                modifier = Modifier.fillMaxSize().padding(8.dp)
            )
        }

        // FAB
        FloatingActionButton(
            onClick = onNavigateToSmartParse,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.medium,
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                    Text("添加", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

private fun startPing(
    address: String,
    useICMP: Boolean,
    pingCount: String,
    pingSize: String,
    pingPort: String,
    scope: CoroutineScope,
    onStart: (Job) -> Unit,
    onLine: (String) -> Unit,
    onTime: (Double) -> Unit,
    onFinish: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onSummary: (String) -> Unit
) {
    val transmitted = mutableStateOf(0)
    val times = mutableListOf<Double>()

    fun generateSummary(): String {
        val received = times.size
        val loss = if (transmitted.value > 0) (transmitted.value - received) * 100.0 / transmitted.value else 0.0
        val min = times.minOrNull() ?: 0.0
        val max = times.maxOrNull() ?: 0.0
        val avg = times.average().takeUnless { it.isNaN() } ?: 0.0
        return buildString {
            appendLine()
            appendLine("--- $address ping statistics ---")
            appendLine("${transmitted.value} packets transmitted, $received received, ${round(loss).toInt()}% packet loss")
            if (received > 0) {
                appendLine("rtt min/avg/max = ${round(min).toInt()}/${round(avg).toInt()}/${round(max).toInt()} ms")
            }
        }
    }

    val job = scope.launch {
        try {
            if (useICMP) {
                val flow = IcmpPing.ping(
                    host = address,
                    count = pingCount.toIntOrNull() ?: 0,
                    packetSize = pingSize.toIntOrNull() ?: 56
                )
                flow.collect { line ->
                    onLine(line)
                    val timeRegex = Regex("time[=<]\\s*([0-9.]+)\\s*ms")
                    timeRegex.find(line)?.let { match ->
                        match.groupValues[1].toDoubleOrNull()?.let { time ->
                            times.add(time)
                            onTime(time)
                        }
                    }
                    if (line.contains("icmp_seq=")) transmitted.value++
                }
            } else {
                val count = pingCount.toIntOrNull() ?: 0
                val port = pingPort.toIntOrNull() ?: 80
                val flow = TcpPing.ping(host = address, count = count, port = port)
                flow.collect { line ->
                    onLine(line)
                    val timeRegex = Regex("time[=<]\\s*([0-9.]+)\\s*ms")
                    timeRegex.find(line)?.let { match ->
                        match.groupValues[1].toDoubleOrNull()?.let { time ->
                            times.add(time)
                            onTime(time)
                        }
                    }
                    if (line.contains("seq=")) transmitted.value++
                }
            }
            onSummary(generateSummary())
            onFinish()
        } catch (e: CancellationException) {
            onSummary(generateSummary())
            onCancel?.invoke()
            onFinish()
        } catch (e: Exception) {
            onLine("\n发生错误: ${e.message}")
            onFinish()
        }
    }
    onStart(job)
}
