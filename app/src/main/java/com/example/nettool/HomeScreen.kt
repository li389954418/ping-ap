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

    // 从 ViewModel 订阅后台 Ping 状态
    val outputLines by viewModel.backgroundPingOutput.collectAsState()
    val pingTimes by viewModel.backgroundPingTimes.collectAsState()
    val isRunning by viewModel.isBackgroundPingRunning.collectAsState()

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
                viewModel.startBackgroundPing(
                    address = autoPingAddress,
                    useICMP = useICMP,
                    pingCount = pingCount.toIntOrNull() ?: 0,
                    pingSize = pingSize.toIntOrNull() ?: 56,
                    pingPort = pingPort.toIntOrNull() ?: 80
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
                                viewModel.startBackgroundPing(
                                    address = targetAddress,
                                    useICMP = useICMP,
                                    pingCount = pingCount.toIntOrNull() ?: 0,
                                    pingSize = pingSize.toIntOrNull() ?: 56,
                                    pingPort = pingPort.toIntOrNull() ?: 80
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
                                    viewModel.startBackgroundPing(
                                        address = entry.address,
                                        useICMP = useICMP,
                                        pingCount = pingCount.toIntOrNull() ?: 0,
                                        pingSize = pingSize.toIntOrNull() ?: 56,
                                        pingPort = pingPort.toIntOrNull() ?: 80
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

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
                        viewModel.stopBackgroundPing()
                    } else {
                        viewModel.startBackgroundPing(
                            address = targetAddress,
                            useICMP = useICMP,
                            pingCount = pingCount.toIntOrNull() ?: 0,
                            pingSize = pingSize.toIntOrNull() ?: 56,
                            pingPort = pingPort.toIntOrNull() ?: 80
                        )
                    }
                },
                enabled = targetAddress.isNotBlank(),
                modifier = Modifier.width(120.dp)
            ) {
                Text(if (isRunning) "停止" else "开始")
            }

            Button(
                onClick = { viewModel.clearBackgroundPingOutput() },
                enabled = outputLines.isNotEmpty() && !isRunning,
                modifier = Modifier.width(120.dp)
            ) {
                Text("清空")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            PingChart(
                times = pingTimes,
                modifier = Modifier.fillMaxSize().padding(8.dp)
            )
        }

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
