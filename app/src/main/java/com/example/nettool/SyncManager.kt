package com.example.nettool

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object SyncManager {
    data class SyncResult(
        val uploaded: Int = 0,
        val downloaded: Int = 0,
        val conflicts: Int = 0,
        val merged: Int = 0,
        val errors: List<String> = emptyList()
    )

    data class ConflictItem(
        val local: IpEntry,
        val remote: IpEntry,
        val field: String,
        val localValue: String,
        val remoteValue: String
    )

    suspend fun syncNow(
        context: Context,
        viewModel: MainViewModel,
        onProgress: (String) -> Unit = {}
    ): SyncResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        var uploaded = 0
        var downloaded = 0
        var conflicts = 0
        var merged = 0

        try {
            onProgress("正在准备同步...")
            val currentUserName = runBlocking {
                runCatching { context.dataStore.data.first()[ThemeManager.USER_NAME] ?: "" }.getOrDefault("")
            }

            val localEntries = viewModel.getAllEntriesSync()
            
            val target = BackupManager.getAutoBackupTarget(context)
            val cloudData = when (target) {
                "webdav" -> {
                    val config = BackupManager.getWebDavConfig(context)
                    if (config != null) {
                        onProgress("正在从坚果云拉取数据...")
                        WebDavClient.downloadLatest(config, "NetTool_Sync_") ?: ""
                    } else ""
                }
                else -> {
                    val files = BackupManager.listBackupFiles(context)
                    files.firstOrNull { it.name.startsWith("NetTool_Sync_") }?.readText() ?: ""
                }
            }

            val cloudEntries = mutableListOf<IpEntry>()
            if (cloudData.isNotBlank()) {
                onProgress("正在解析云端数据...")
                try {
                    val cloudJson = JSONObject(cloudData)
                    val cloudArray = cloudJson.optJSONArray("entries") ?: JSONArray()
                    for (i in 0 until cloudArray.length()) {
                        val obj = cloudArray.getJSONObject(i)
                        cloudEntries.add(IpEntry(
                            id = obj.optInt("id", 0),
                            name = obj.optString("name"),
                            address = obj.optString("address"),
                            extraRemarks = obj.optString("extraRemarks", "{}"),
                            category = obj.optString("category", "互联网"),
                            createdAt = obj.optLong("createdAt", 0),
                            updatedAt = obj.optLong("updatedAt", 0),
                            deleted = obj.optBoolean("deleted", false),
                            userName = obj.optString("userName", "")
                        ))
                    }
                } catch (e: Exception) {
                    errors.add("解析云端数据失败: ${e.message}")
                }
            }

            onProgress("正在合并数据...")
            val mergedEntries = mutableListOf<IpEntry>()
            val localMap = localEntries.associateBy { it.id }
            val cloudMap = cloudEntries.associateBy { it.id }

            localEntries.forEach { local ->
                val cloud = cloudMap[local.id]
                if (cloud == null) {
                    mergedEntries.add(local)
                    uploaded++
                } else {
                    if (local.updatedAt > cloud.updatedAt) {
                        mergedEntries.add(local)
                        uploaded++
                    } else if (cloud.updatedAt > local.updatedAt) {
                        mergedEntries.add(cloud.copy(userName = cloud.userName.ifBlank { "云端" }))
                        downloaded++
                    } else {
                        mergedEntries.add(local)
                    }
                }
            }

            cloudEntries.forEach { cloud ->
                if (!localMap.containsKey(cloud.id)) {
                    mergedEntries.add(cloud.copy(userName = cloud.userName.ifBlank { "云端" }))
                    downloaded++
                }
            }

            onProgress("正在保存数据...")
            viewModel.replaceAllEntries(mergedEntries)

            onProgress("正在上传同步结果...")
            val exportData = viewModel.exportEntries(mergedEntries)
            val fileName = "NetTool_Sync_${System.currentTimeMillis()}.json"
            when (target) {
                "webdav" -> {
                    val config = BackupManager.getWebDavConfig(context)
                    if (config != null) {
                        WebDavClient.upload(config, fileName, exportData)
                    }
                }
                else -> {
                    BackupManager.saveToFile(context, exportData, fileName)
                }
            }

            onProgress("同步完成！上传 $uploaded 条，下载 $downloaded 条")
        } catch (e: Exception) {
            errors.add("同步失败: ${e.message}")
        }

        SyncResult(
            uploaded = uploaded,
            downloaded = downloaded,
            conflicts = conflicts,
            merged = merged,
            errors = errors
        )
    }
}
