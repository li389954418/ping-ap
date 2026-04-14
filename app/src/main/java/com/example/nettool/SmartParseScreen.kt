package com.example.nettool
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartParseScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var route by remember { mutableStateOf("") }
    var imsLocator by remember { mutableStateOf("") }
    var imsRawText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var previewEntries by remember { mutableStateOf<List<IpEntry>>(emptyList()) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<IpEntry?>(null) }
    var mainRemark by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("互联网") }
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    var categoryExpanded by remember { mutableStateOf(false) }
    fun startEditing(entry: IpEntry) {
        editingEntry = entry
        mainRemark = entry.name
        val json = try {
            JSONObject(entry.extraRemarks)
        } catch (e: Exception) {
            JSONObject()
        }
        customerAddress = json.optString("地址", "").ifBlank { json.optString("address", "") }
        selectedCategory = entry.category
    }
    fun saveCurrentEntry() {
        editingEntry?.let { entry ->
            val json = JSONObject()
            try {
                val oldJson = JSONObject(entry.extraRemarks)
                oldJson.keys().forEach { key ->
                    if (key.matches(Regex("IP\\d+"))) {
                        json.put(key, oldJson.get(key))
                    }
                }
            } catch (_: Exception) {}
            if (customerAddress.isNotBlank()) {
                json.put("地址", customerAddress)
            }
            if (route.isNotBlank()) {
                json.put("route", route)
            }
            val updatedEntry = entry.copy(
                name = mainRemark,
                extraRemarks = json.toString(),
                category = selectedCategory
            )
            viewModel.batchSaveEntries(listOf(updatedEntry))
        }
        showConfirmDialog = false
        showQuickAddDialog = false
        onBack()
    }
    fun startQuickAdd() {
        editingEntry = IpEntry(name = "", address = "", extraRemarks = "{}", category = "互联网")
        mainRemark = ""
        customerAddress = ""
        selectedCategory = "互联网"
        showQuickAddDialog = true
    }
    fun handleRecognize() {
        if (imsRawText.isBlank()) {
            Toast.makeText(context, "请输入原始文本", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            val lines = imsRawText.lines().filter { it.isNotBlank() }
            val isBatch = lines.size > 1 || (lines.size == 1 && lines[0].trim().split(Regex("\\s+")).size >= 2)
            if (isBatch) {
                var success = 0
                var fail = 0
                for (line in lines) {
                    val parts = line.trim().split(Regex("\\s+"), limit = 2)
                    if (parts.size < 2) {
                        fail++
                        continue
                    }
                    val locator = parts[0]
                    val raw = parts[1]
                    val target = viewModel.findImsEntry(locator)
                    if (target == null) {
                        fail++
                        continue
                    }
                    val (port, number, password) = viewModel.parseImsInfo(raw)
                    if (port.isBlank() && number.isBlank() && password.isBlank()) {
                        fail++
                        continue
                    }
                    viewModel.updateImsEntry(target, port, number, password)
                    success++
                }
                Toast.makeText(context, "批量完成: 成功 $success 条, 失败 $fail 条", Toast.LENGTH_LONG).show()
            } else {
                if (imsLocator.isBlank()) {
                    Toast.makeText(context, "请填写定位信息", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val target = viewModel.findImsEntry(imsLocator)
                if (target == null) {
                    Toast.makeText(context, "未找到匹配的 IMS 记录", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val (port, number, password) = viewModel.parseImsInfo(imsRawText)
                if (port.isBlank() && number.isBlank() && password.isBlank()) {
                    Toast.makeText(context, "未能识别到有效信息", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                viewModel.updateImsEntry(target, port, number, password)
                Toast.makeText(context, "已更新 IMS 记录: ${target.name}", Toast.LENGTH_SHORT).show()
                imsLocator = ""
            }
            imsRawText = ""
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("智能解析", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("粘贴文档内容") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            maxLines = 8
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = route,
            onValueChange = { route = it },
            label = { Text("路由（非必填）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    isProcessing = true
                    previewEntries = viewModel.autoParseAndPreview(inputText, selectedCategory)
                    isProcessing = false
                    if (previewEntries.isNotEmpty()) {
                        startEditing(previewEntries.first())
                        showConfirmDialog = true
                    } else {
                        Toast.makeText(context, "未提取到有效信息", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = inputText.isNotBlank() && !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("智能解析")
                }
            }
            Button(
                onClick = { startQuickAdd() },
                modifier = Modifier.weight(1f)
            ) {
                Text("快速添加")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("📞 号码保存", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = imsLocator,
            onValueChange = { imsLocator = it },
            label = { Text("输入 IP 或产品实例标识定位 IMS 记录（单条时必填）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = imsRawText,
            onValueChange = { imsRawText = it },
            label = { Text("粘贴文本（单条或批量，批量时每行格式: 定位标识 原始文本）") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 5
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { handleRecognize() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔍 识别并更新")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
    if (showConfirmDialog && editingEntry != null) {
        EditEntryDialog(
            entry = editingEntry!!,
            mainRemark = mainRemark,
            onMainRemarkChange = { mainRemark = it },
            customerAddress = customerAddress,
            onCustomerAddressChange = { customerAddress = it },
            selectedCategory = selectedCategory,
            onCategoryChange = { selectedCategory = it },
            categories = categories,
            onSave = { saveCurrentEntry() },
            onDismiss = { showConfirmDialog = false }
        )
    }
    if (showQuickAddDialog && editingEntry != null) {
        EditEntryDialog(
            entry = editingEntry!!,
            mainRemark = mainRemark,
            onMainRemarkChange = { mainRemark = it },
            customerAddress = customerAddress,
            onCustomerAddressChange = { customerAddress = it },
            selectedCategory = selectedCategory,
            onCategoryChange = { selectedCategory = it },
            categories = categories,
            onSave = { saveCurrentEntry() },
            onDismiss = { showQuickAddDialog = false }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryDialog(
    entry: IpEntry,
    mainRemark: String,
    onMainRemarkChange: (String) -> Unit,
    customerAddress: String,
    onCustomerAddressChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    categories: List<String>,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var currentAddress by remember { mutableStateOf(entry.address) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑信息") },
        text = {
            Column {
                OutlinedTextField(
                    value = mainRemark,
                    onValueChange = onMainRemarkChange,
                    label = { Text("客户名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentAddress,
                    onValueChange = { currentAddress = it },
                    label = { Text("IP 地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customerAddress,
                    onValueChange = onCustomerAddressChange,
                    label = { Text("客户地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    onCategoryChange(category)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
