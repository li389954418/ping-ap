package com.example.nettool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PingApp()
            }
        }
    }
}

@Composable
fun PingApp(viewModel: MainViewModel = viewModel()) {
    val entries by viewModel.entries.collectAsState()
    val pingResult by viewModel.pingResult.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var quickAddress by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "⚡ 快速 Ping",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quickAddress,
                    onValueChange = { quickAddress = it },
                    label = { Text(text = "输入 IP 或域名") },
                    placeholder = { Text(text = "例如 8.8.8.8 或 baidu.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { if (quickAddress.isNotBlank()) viewModel.pingAddress(quickAddress) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = "开始 Ping")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pingResult.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = pingResult,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Divider()

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "📋 保存常用地址", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text(text = "🔍 搜索备注或IP") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text(text = "备注名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = newAddress,
            onValueChange = { newAddress = it },
            label = { Text(text = "IP 地址或域名") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (newName.isNotBlank() && newAddress.isNotBlank()) {
                    viewModel.addEntry(newName, newAddress)
                    newName = ""
                    newAddress = ""
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = "保存到列表")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(entries) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = entry.name, style = MaterialTheme.typography.bodyLarge)
                            Text(text = entry.address, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row {
                            TextButton(onClick = { viewModel.pingAddress(entry.address) }) {
                                Text(text = "Ping")
                            }
                            TextButton(onClick = { viewModel.deleteEntry(entry) }) {
                                Text(text = "删除", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}