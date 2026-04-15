package com.example.nettool

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
    var currentAddress by remember { mutableStateOf("") }

    var remarkItems by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    var duplicateData by remember { mutableStateOf<Triple<IpEntry, IpEntry, TemplateEntry>?>(null) }
    var showDuplicateDialog by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val templates by viewModel.templates.collectAsState(initial = emptyList())
    var categoryExpanded by remember { mutableStateOf(false) }

    fun startEditing(entry: IpEntry) {
        editingEntry = entry
        mainRemark = entry.name
        currentAddress = entry.address
        val json = try { JSONObject(entry.extraRemarks) } catch (e: Exception) { JSONObject() }
        customerAddress = json.optString("地址", "").ifBlank { json.optString("address", "") }
        selectedCategory = entry.category

        val items = mutableListOf<Pair<String, String>>()
        json.keys().forEach { key ->
            when {
                key == "地址" || key == "address" || key.matches(Regex("IP\\d+")) ||
                key == "ims_port" || key == "ims_number" || key == "ims_password" ||
                key == "route" -> { }
                else -> items.add(key to json.optString(key, ""))
            }
        }
        remarkItems = items
    }

    fun saveCurrentEntry(closeAfter: Boolean = true) {
        editingEntry?.let { entry ->
            val json = JSONObject()
            try {
                val oldJson = JSONObject(entry.extraRemarks)
                oldJson.keys().forEach { key ->
                    if (key.matches(Regex("IP\\d+"))) json.put(key, oldJson.get(key))
                }
            } catch (_: Exception) {}
            if (customerAddress.isNotBlank()) json.put("地址", customerAddress)
            if (route.isNotBlank()) json.put("route", route)
            remarkItems.forEach { (key, value) -> if (key.isNotBlank()) json.put(key, value) }

            val updatedEntry = entry.copy(
                name = mainRemark,
                address = currentAddress,  // 关键：使用 currentAddress
                extraRemarks = json.toString(),
                category = selectedCategory
            )

            scope.launch {
                val enabledTemplates = templates.filter { it.enabled }
                val template = enabledTemplates.firstOrNull()
                if (template != null) {
                    val duplicate = viewModel.findDuplicateEntryByTemplate(updatedEntry, template)
                    if (duplicate != null) {
                        duplicateData = Triple(duplicate, updatedEntry, template)
                        showDuplicateDialog = true
                        return@launch
                    }
                }
                viewModel.batchSaveEntries(listOf(updatedEntry))
                if (closeAfter) {
                    showConfirmDialog = false
                    showQuickAddDialog = false
                    onBack()
                } else {
                    previewEntries = emptyList()
                    editingEntry = null
                    showConfirmDialog = false
                    showQuickAddDialog = false
                }
            }
        }
    }

    fun startQuickAdd() {
        editingEntry = IpEntry(name = "", address = "", extraRemarks = "{}", category = "互联网")
        mainRemark = ""
        currentAddress = ""
        customerAddress = ""
        selectedCategory = "互联网"
        remarkItems = emptyList()
        showQuickAddDialog = true
    }

    // 其余代码保持不变...
    // 由于篇幅，此处省略与之前相同的 handleRecognize 和 UI 部分
    // 实际使用时请确保包含完整代码
}
